import {
  questionnaires,
  questions,
  submissions,
  submissionAnswers,
} from "../db/schema/questionnaires.ts";
import { db } from "../db/index.ts";
import { eq } from "drizzle-orm";
import type { InferInsertModel, InferSelectModel } from "drizzle-orm";

/**
 * Type used when inserting a new questionnaire into the database.
 */
export type QuestionnaireInsert = InferInsertModel<typeof questionnaires>;

/**
 * Type representing a questionnaire record selected from the database.
 */
export type QuestionnaireSelect = InferSelectModel<typeof questionnaires>;

/**
 * Type used when inserting a new question into the database.
 */
export type QuestionInsert = InferInsertModel<typeof questions>;

/**
 * Type representing a question record selected from the database.
 */
export type QuestionSelect = InferSelectModel<typeof questions>;

/**
 * Type used when inserting a new submission into the database.
 */
export type SubmissionInsert = InferInsertModel<typeof submissions>;

/**
 * Type representing a submission record selected from the database.
 */
export type SubmissionSelect = InferSelectModel<typeof submissions>;

/**
 * Type used when inserting a new submission answer into the database.
 */
export type SubmissionAnswerInsert = InferInsertModel<typeof submissionAnswers>;

/**
 * Type representing a submission answer record selected from the database.
 */
export type SubmissionAnswerSelect = InferSelectModel<typeof submissionAnswers>;

/**
 * Type representing a full questionnaire, which includes the questionnaire record along with its associated questions.
 */
export type FullQuestionnaire = QuestionnaireSelect & { questions: QuestionSelect[] };

export namespace Questionnaires {
  /**
   * Creates a new questionnaire along with its associated questions.
   *
   * This function validates the provided title and questions array. It then creates a new questionnaire record with the
   * current timestamp and inserts each provided question into the database, linking them to the created questionnaire.
   *
   * @param title - The title of the questionnaire.
   * @param description - A brief description of the questionnaire.
   * @param questionsInput - An array of objects, each containing a 'questionText' property for the question.
   * @returns A promise that resolves to the full questionnaire, including the inserted questions.
   * @throws An error if the title is empty or if the questions array is missing or empty.
   */
  export async function createQuestionnaire(
    title: string,
    description: string,
    questionsInput: { questionText: string }[]
  ): Promise<FullQuestionnaire> {
    if (
      !title ||
      !questionsInput ||
      !Array.isArray(questionsInput) ||
      questionsInput.length === 0
    ) {
      throw new Error("Title and a non-empty questions array are required.");
    }
    
    const createdAt = new Date().toISOString();
    
    const [insertedQuestionnaire] = await db
      .insert(questionnaires)
      .values({
        title,
        description,
        createdAt,
      })
      .returning();
    
    const questionsWithId = await Promise.all(
      questionsInput.map(async (q) => {
        const [insertedQuestion] = await db
          .insert(questions)
          .values({
            questionnaireId: insertedQuestionnaire.id,
            questionText: q.questionText,
          })
          .returning();
        return insertedQuestion;
      })
    );
    
    return {
      ...insertedQuestionnaire,
      questions: questionsWithId,
    };
  }

  /**
   * Retrieves all questionnaires in a summary view.
   *
   * This function returns an array of questionnaire records from the database without including their associated questions.
   *
   * @returns A promise that resolves to an array of questionnaires.
   */
  export async function getAllQuestionnaires(): Promise<QuestionnaireSelect[]> {
    return await db.select().from(questionnaires);
  }

  /**
   * Retrieves a specific questionnaire along with its questions.
   *
   * This function fetches a questionnaire by its ID. If found, it then retrieves all associated questions and returns
   * a combined object. If the questionnaire is not found, it returns undefined.
   *
   * @param questionnaireId - The unique identifier of the questionnaire.
   * @returns A promise that resolves to the full questionnaire (including its questions) or undefined if not found.
   */
  export async function getQuestionnaire(questionnaireId: number): Promise<FullQuestionnaire | undefined> {
    const [qn] = await db
      .select()
      .from(questionnaires)
      .where(eq(questionnaires.id, questionnaireId));
    
    if (!qn) return undefined;
    
    const qs = await db
      .select()
      .from(questions)
      .where(eq(questions.questionnaireId, questionnaireId));
    
    return {
      ...qn,
      questions: qs,
    };
  }

  /**
   * Submits answers for a given questionnaire.
   *
   * This function verifies that the specified questionnaire exists. It then creates a submission record for the
   * provided student along with the current timestamp and a status of "submitted". For each answer provided,
   * it creates a corresponding submission answer record linked to the submission.
   *
   * @param questionnaireId - The ID of the questionnaire for which answers are being submitted.
   * @param studentId - The ID of the student submitting the answers.
   * @param answersInput - An array of objects, each containing a questionId and the answer as a string.
   * @returns A promise that resolves to the inserted submission record.
   * @throws An error if the questionnaire is not found.
   */
  export async function submitAnswers(
    questionnaireId: number,
    studentId: number,
    answersInput: { questionId: number; answer: string }[]
  ): Promise<SubmissionSelect> {
    const [qn] = await db
      .select()
      .from(questionnaires)
      .where(eq(questionnaires.id, questionnaireId));
    
    if (!qn) {
      throw new Error("Questionnaire not found.");
    }
    
    const submittedAt = new Date().toISOString();
    
    const [insertedSubmission] = await db
      .insert(submissions)
      .values({
        questionnaireId,
        studentId,
        submittedAt,
        status: "submitted",
      })
      .returning();
    
    await Promise.all(
      answersInput.map(({ questionId, answer }) =>
        db.insert(submissionAnswers)
          .values({
            submissionId: insertedSubmission.submissionId,
            questionId,
            answer,
          })
          .returning()
      )
    );
    
    return insertedSubmission;
  }
}
