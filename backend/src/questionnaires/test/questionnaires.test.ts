import { describe, test, beforeEach, expect } from "vitest";
import { Questionnaires } from "../questionnaires.ts";
import { db } from "../../db/index.ts";
import {
  questionnaires,
  questions,
  submissions,
  submissionAnswers,
} from "../../db/schema/questionnaires.ts";

beforeEach(async () => {
  await db.delete(submissionAnswers).execute();
  await db.delete(submissions).execute();
  await db.delete(questions).execute();
  await db.delete(questionnaires).execute();
});

describe("Questionnaires Module", () => {
  describe("createQuestionnaire", () => {
    test("creates a questionnaire with associated questions", async () => {
      const title = "Sample Questionnaire";
      const description = "A simple test questionnaire";
      const questionsInput = [
        { questionText: "What is your name?" },
        { questionText: "What is your quest?" },
      ];

      const fullQuestionnaire = await Questionnaires.createQuestionnaire(
        title,
        description,
        questionsInput
      );

      // Validate questionnaire record
      expect(fullQuestionnaire).toHaveProperty("id");
      expect(fullQuestionnaire.title).toBe(title);
      expect(fullQuestionnaire.description).toBe(description);
      expect(fullQuestionnaire).toHaveProperty("createdAt");
      expect(Array.isArray(fullQuestionnaire.questions)).toBe(true);
      expect(fullQuestionnaire.questions.length).toBe(2);

      // Validate each inserted question
      expect(fullQuestionnaire.questions[0].questionText).toBe(
        questionsInput[0].questionText
      );
      expect(fullQuestionnaire.questions[1].questionText).toBe(
        questionsInput[1].questionText
      );
    });

    test("throws an error if title is empty", async () => {
      await expect(
        Questionnaires.createQuestionnaire("", "desc", [
          { questionText: "Question?" },
        ])
      ).rejects.toThrow("Title and a non-empty questions array are required.");
    });

    test("throws an error if questions array is empty", async () => {
      await expect(
        Questionnaires.createQuestionnaire("Title", "desc", [])
      ).rejects.toThrow("Title and a non-empty questions array are required.");
    });
  });

  describe("getAllQuestionnaires", () => {
    test("returns an array of questionnaires", async () => {
      // Create a questionnaire first
      const title = "Test Questionnaire";
      const description = "Test description";
      const questionsInput = [{ questionText: "Test question?" }];
      const createdQuestionnaire = await Questionnaires.createQuestionnaire(
        title,
        description,
        questionsInput
      );

      const allQuestionnaires = await Questionnaires.getAllQuestionnaires();
      expect(Array.isArray(allQuestionnaires)).toBe(true);
      expect(allQuestionnaires.length).toBeGreaterThan(0);

      // Check that the created questionnaire is in the list
      const found = allQuestionnaires.find(
        (q) => q.id === createdQuestionnaire.id
      );
      expect(found).toBeDefined();
      expect(found!.title).toBe(title);
    });
  });

  describe("getQuestionnaire", () => {
    test("returns a full questionnaire with its questions if found", async () => {
      // Create a questionnaire first
      const title = "Detailed Questionnaire";
      const description = "Detailed description";
      const questionsInput = [
        { questionText: "What is your favorite color?" },
      ];
      const createdQuestionnaire = await Questionnaires.createQuestionnaire(
        title,
        description,
        questionsInput
      );

      const fetchedQuestionnaire = await Questionnaires.getQuestionnaire(
        createdQuestionnaire.id
      );
      expect(fetchedQuestionnaire).toBeDefined();
      expect(fetchedQuestionnaire!.id).toBe(createdQuestionnaire.id);
      expect(Array.isArray(fetchedQuestionnaire!.questions)).toBe(true);
      expect(fetchedQuestionnaire!.questions.length).toBe(1);
      expect(fetchedQuestionnaire!.questions[0].questionText).toBe(
        questionsInput[0].questionText
      );
    });

    test("returns undefined if the questionnaire is not found", async () => {
      const fetchedQuestionnaire = await Questionnaires.getQuestionnaire(9999);
      expect(fetchedQuestionnaire).toBeUndefined();
    });
  });

  describe("submitAnswers", () => {
    test("submits answers and returns a submission record", async () => {
      // Create a questionnaire with questions first
      const title = "Submission Questionnaire";
      const description = "Submission test";
      const questionsInput = [
        { questionText: "What is your favorite sport?" },
        { questionText: "What is your hometown?" },
      ];
      const createdQuestionnaire = await Questionnaires.createQuestionnaire(
        title,
        description,
        questionsInput
      );

      // Prepare answers using the created question ids
      const answersInput = createdQuestionnaire.questions.map((q) => ({
        questionId: q.id,
        answer: `Answer for question ${q.id}`,
      }));

      const studentId = 1;
      const submission = await Questionnaires.submitAnswers(
        createdQuestionnaire.id,
        studentId,
        answersInput
      );

      expect(submission).toHaveProperty("submissionId");
      expect(submission.studentId).toBe(studentId);
      expect(submission.status).toBe("submitted");
    });

    test("throws an error if the questionnaire does not exist", async () => {
      const studentId = 1;
      await expect(
        Questionnaires.submitAnswers(9999, studentId, [
          { questionId: 1, answer: "Test answer" },
        ])
      ).rejects.toThrow("Questionnaire not found.");
    });
  });
});
