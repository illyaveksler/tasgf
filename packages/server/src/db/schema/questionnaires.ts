import {
    pgTable,
    serial,
    text,
    timestamp,
    integer,
  } from 'drizzle-orm/pg-core';
  
  export const questionnaires = pgTable('questionnaires', {
    id: serial('id').primaryKey(),
    title: text('title').notNull(),
    description: text('description').notNull(),
    createdAt: timestamp('created_at', { mode: 'string' }).notNull().defaultNow(),
  });
  
  export const questions = pgTable('questions', {
    id: serial('id').primaryKey(),
    questionnaireId: integer('questionnaire_id').notNull(),
    questionText: text('question_text').notNull(),
  });

  export const submissions = pgTable('submissions', {
    submissionId: serial('submission_id').primaryKey(),
    questionnaireId: integer('questionnaire_id').notNull(),
    submittedAt: timestamp('submitted_at', { mode: 'string' }).notNull().defaultNow(),
    status: text('status').notNull(),
  });

  export const submissionAnswers = pgTable('submission_answers', {
    id: serial('id').primaryKey(),
    submissionId: integer('submission_id'),
    questionId: integer('question_id'),
    answer: text('answer'),
  });
  