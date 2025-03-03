CREATE TABLE "questionnaires" (
	"id" serial PRIMARY KEY NOT NULL,
	"title" text NOT NULL,
	"description" text NOT NULL,
	"created_at" timestamp DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "questions" (
	"id" serial PRIMARY KEY NOT NULL,
	"questionnaire_id" integer NOT NULL,
	"question_text" text NOT NULL
);
--> statement-breakpoint
CREATE TABLE "submission_answers" (
	"id" serial PRIMARY KEY NOT NULL,
	"submission_id" integer,
	"question_id" integer,
	"answer" text
);
--> statement-breakpoint
CREATE TABLE "submissions" (
	"submission_id" serial PRIMARY KEY NOT NULL,
	"questionnaire_id" integer NOT NULL,
	"submitted_at" timestamp DEFAULT now() NOT NULL,
	"status" text NOT NULL
);
