import { db } from "../db/index.ts";
import { submissions, submissionAnswers, student_groups } from "../db/schema/questionnaires.ts";
import { eq, inArray, desc } from "drizzle-orm";

export namespace StudentGroups {
  /**
   * Generates groups of student IDs for a given questionnaire with balanced average scores.
   * Each submission's average answer (a number between 1 and 5) is computed, then
   * submissions are sorted in descending order by average.
   *
   * To balance the groups, we:
   * 1. Determine the number of groups as the ceiling of (total submissions / groupSize).
   * 2. Initialize each group with an empty list and a sum of 0.
   * 3. Iterate over the sorted submissions and assign each one to the group (that is not full) with the lowest current total score.
   *
   * This greedy algorithm attempts to equalize the overall group sums (and hence averages).
   *
   * @param questionnaireId The questionnaire to process.
   * @param groupSize The desired number of students per group (must be at least 1).
   * @returns An array of groups; each group is an array of student IDs.
   */
  export async function generateGroups(
    questionnaireId: number,
    groupSize: number
  ): Promise<number[][]> {
    if (groupSize < 1) {
      throw new Error("Group size must be at least 1.");
    }

    // Retrieve all submissions for the given questionnaire.
    const submissionsList = await db
      .select()
      .from(submissions)
      .where(eq(submissions.questionnaireId, questionnaireId));

    if (submissionsList.length === 0) {
      throw new Error("No submissions found for this questionnaire.");
    }

    // Extract submission IDs.
    const submissionIds = submissionsList.map((sub) => sub.submissionId);

    // Retrieve all answers for these submissions.
    const answersList = await db
      .select()
      .from(submissionAnswers)
      .where(inArray(submissionAnswers.submissionId, submissionIds));

    // Map each submissionId to its array of answers (converted to numbers).
    const submissionAnswersMap: Record<number, number[]> = {};
    for (const answerRecord of answersList) {
      if (!submissionAnswersMap[answerRecord.submissionId]) {
        submissionAnswersMap[answerRecord.submissionId] = [];
      }
      submissionAnswersMap[answerRecord.submissionId].push(
        parseInt(answerRecord.answer, 10)
      );
    }

    // Compute the overall average answer for each submission.
    const submissionsWithAverage = submissionsList.map((sub) => {
      const answers = submissionAnswersMap[sub.submissionId] || [];
      const avg = answers.length > 0
        ? answers.reduce((sum, val) => sum + val, 0) / answers.length
        : 0;
      return { ...sub, average: avg };
    });

    // Sort submissions by average descending.
    submissionsWithAverage.sort((a, b) => b.average - a.average);

    // Determine the number of groups.
    const numGroups = Math.ceil(submissionsWithAverage.length / groupSize);

    // Initialize groups. Each group holds an array of student IDs and a running total of averages.
    const groups: { studentIds: number[]; sum: number }[] = [];
    for (let i = 0; i < numGroups; i++) {
      groups.push({ studentIds: [], sum: 0 });
    }

    // Greedily assign each submission to the group with the lowest total that is not full.
    for (const sub of submissionsWithAverage) {
      let targetGroupIndex = -1;
      let minSum = Infinity;
      for (let i = 0; i < groups.length; i++) {
        if (groups[i].studentIds.length < groupSize) {
          if (groups[i].sum < minSum) {
            minSum = groups[i].sum;
            targetGroupIndex = i;
          }
        }
      }
      if (targetGroupIndex === -1) {
        // This should not occur because at least one group will have room.
        throw new Error("All groups are full unexpectedly.");
      }
      groups[targetGroupIndex].studentIds.push(sub.studentId);
      groups[targetGroupIndex].sum += sub.average;
    }

    return groups.map((g) => g.studentIds);
  }

  /**
   * Generates groups for a questionnaire, stores them in the database, and returns the groups.
   * @param questionnaireId The questionnaire id.
   * @param groupSize The desired group size.
   * @returns The generated groups (an array of arrays of student IDs).
   */
  export async function generateAndStoreGroups(
    questionnaireId: number,
    groupSize: number
  ): Promise<number[][]> {
    const groups = await generateGroups(questionnaireId, groupSize);
    // Store the generated groups in the student_groups table.
    await db.insert(student_groups).values({
      questionnaireId,
      groups,
    });
    return groups;
  }

  /**
   * Retrieves the most recently stored groups for a given questionnaire.
   * @param questionnaireId The questionnaire id.
   * @returns The stored groups (an array of arrays of student IDs).
   */
  export async function getStoredGroups(
    questionnaireId: number
  ): Promise<number[][]> {
    const [record] = await db
      .select()
      .from(student_groups)
      .where(eq(student_groups.questionnaireId, questionnaireId))
      .orderBy(desc(student_groups.createdAt))
      .limit(1);

    if (!record) {
      throw new Error("No stored groups found for this questionnaire.");
    }
    return record.groups as number[][];
  }
}
