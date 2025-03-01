import {
  questionnaires,
  questions,
  submissions,
  submissionAnswers,
} from "../db/schema/questionnaires.ts";
import { db } from "../db/index.ts";
import { eq } from "drizzle-orm";
import type { InferInsertModel, InferSelectModel } from "drizzle-orm";

export type QuestionnaireInsert = InferInsertModel<typeof questionnaires>;
export type QuestionnaireSelect = InferSelectModel<typeof questionnaires>;
export type QuestionInsert = InferInsertModel<typeof questions>;
export type QuestionSelect = InferSelectModel<typeof questions>;
export type SubmissionInsert = InferInsertModel<typeof submissions>;
export type SubmissionSelect = InferSelectModel<typeof submissions>;
export type SubmissionAnswerInsert = InferInsertModel<typeof submissionAnswers>;
export type SubmissionAnswerSelect = InferSelectModel<typeof submissionAnswers>;

export type FullQuestionnaire = QuestionnaireSelect & { questions: QuestionSelect[] };

export namespace Questionnaires {
  // Create a new questionnaire with its questions
  export async function createQuestionnaire(
    title: string,
    description: string,
    questionsInput: { questionText: string }[]
  ): Promise<FullQuestionnaire> {
    if (!title || !questionsInput || !Array.isArray(questionsInput) || questionsInput.length === 0) {
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

  // Retrieve all questionnaires (summary view without questions)
  export async function getAllQuestionnaires(): Promise<QuestionnaireSelect[]> {
    return await db.select().from(questionnaires);
  }

  // Retrieve a specific questionnaire with its questions
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

  // Submit answers for a given questionnaire
  export async function submitAnswers(
    questionnaireId: number,
    answersInput: { questionId: number; answer: string }[]
  ): Promise<SubmissionSelect> {
    // Validate that the questionnaire exists
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
        submittedAt,
        status: "submitted",
      })
      .returning();
    
    for (const { questionId, answer } of answersInput) {
      await db
        .insert(submissionAnswers)
        .values({
          submissionId: insertedSubmission.submissionId,
          questionId,
          answer,
        })
        .returning();
    }
    
    return insertedSubmission;
  }
}
