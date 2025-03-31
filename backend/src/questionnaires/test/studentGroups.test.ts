import { describe, test, beforeEach, expect } from "vitest";
import { StudentGroups } from "../studentGroups"; // adjust path as needed
import { db } from "../../db/index.ts";
import {
  submissions,
  submissionAnswers,
  student_groups,
  questions,
  questionnaires,
} from "../../db/schema/questionnaires.ts";

// Global counter to ensure each submission gets a unique ID in tests.
let submissionIdCounter = 1000;

/**
 * Helper function to create a submission along with its answers.
 * Each answer is stored as a string, which will be parsed into a number by the grouping logic.
 */
async function createSubmission(
  questionnaireId: number,
  studentId: number,
  answers: string[]
) {
  // Explicitly assign a unique submissionId
  const submissionId = submissionIdCounter++;
  const [sub] = await db
    .insert(submissions)
    .values({
      submissionId, // explicit unique ID
      questionnaireId,
      studentId,
      submittedAt: new Date().toISOString(),
      status: "submitted",
    })
    .returning();

  // Insert an answer record for each provided answer.
  // Here we use a dummy questionId (e.g., 1) because the grouping logic only cares about the answer values.
  for (const answer of answers) {
    await db
      .insert(submissionAnswers)
      .values({
        submissionId: sub.submissionId,
        questionId: 1,
        answer,
      })
      .returning();
  }
  return sub;
}

// Clear all relevant tables before each test.
beforeEach(async () => {
  await db.delete(student_groups).execute();
  await db.delete(submissionAnswers).execute();
  await db.delete(submissions).execute();
  await db.delete(questions).execute();
  await db.delete(questionnaires).execute();
});

describe("StudentGroups.generateGroups", () => {
  test("throws an error if group size is less than 1", async () => {
    await expect(StudentGroups.generateGroups(1, 0)).rejects.toThrow(
      "Group size must be at least 1."
    );
  });

  test("throws an error if no submissions are found for the questionnaire", async () => {
    await expect(StudentGroups.generateGroups(999, 2)).rejects.toThrow(
      "No submissions found for this questionnaire."
    );
  });

  test("returns groups with all student IDs assigned correctly", async () => {
    const questionnaireId = 1;
    // Insert 5 submissions with known answer values:
    // Student 101: answers "5", "5" (average 5)
    // Student 102: answers "3", "3" (average 3)
    // Student 103: answer "4" (average 4)
    // Student 104: answer "2" (average 2)
    // Student 105: answer "1" (average 1)
    await createSubmission(questionnaireId, 101, ["5", "5"]);
    await createSubmission(questionnaireId, 102, ["3", "3"]);
    await createSubmission(questionnaireId, 103, ["4"]);
    await createSubmission(questionnaireId, 104, ["2"]);
    await createSubmission(questionnaireId, 105, ["1"]);

    const groupSize = 2;
    const groups = await StudentGroups.generateGroups(questionnaireId, groupSize);

    // Expected groups: Math.ceil(5 / 2) = 3 groups.
    expect(groups.length).toBe(Math.ceil(5 / groupSize));

    // Each group should have no more than 2 students.
    groups.forEach((group) => {
      expect(group.length).toBeLessThanOrEqual(groupSize);
    });

    // Verify that all student IDs (101, 102, 103, 104, 105) appear exactly once.
    const allStudentIds = groups.flat().sort((a, b) => a - b);
    expect(allStudentIds).toEqual([101, 102, 103, 104, 105]);
  });
});

describe("StudentGroups.generateAndStoreGroups & getStoredGroups", () => {
  test("stores generated groups and retrieves them correctly", async () => {
    const questionnaireId = 2;
    // Insert 3 submissions for questionnaireId 2.
    await createSubmission(questionnaireId, 201, ["4", "4"]); // average 4
    await createSubmission(questionnaireId, 202, ["2", "2"]); // average 2
    await createSubmission(questionnaireId, 203, ["3"]);        // average 3

    const groupSize = 2;
    const generatedGroups = await StudentGroups.generateAndStoreGroups(
      questionnaireId,
      groupSize
    );
    expect(Array.isArray(generatedGroups)).toBe(true);

    // Retrieve the stored groups.
    const storedGroups = await StudentGroups.getStoredGroups(questionnaireId);
    expect(storedGroups).toEqual(generatedGroups);
  });

  test("throws an error when trying to retrieve stored groups for a questionnaire with no groups", async () => {
    const questionnaireId = 999;
    await expect(
      StudentGroups.getStoredGroups(questionnaireId)
    ).rejects.toThrow("No stored groups found for this questionnaire.");
  });
});
