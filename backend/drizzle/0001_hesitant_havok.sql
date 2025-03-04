CREATE TABLE "student_groups" (
	"id" serial PRIMARY KEY NOT NULL,
	"questionnaire_id" integer NOT NULL,
	"groups" json NOT NULL,
	"created_at" timestamp DEFAULT now() NOT NULL
);
--> statement-breakpoint
ALTER TABLE "submission_answers" ALTER COLUMN "submission_id" SET NOT NULL;--> statement-breakpoint
ALTER TABLE "submission_answers" ALTER COLUMN "question_id" SET NOT NULL;--> statement-breakpoint
ALTER TABLE "submission_answers" ALTER COLUMN "answer" SET NOT NULL;--> statement-breakpoint
ALTER TABLE "submissions" ADD COLUMN "student_id" integer NOT NULL;