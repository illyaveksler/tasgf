import { drizzle } from "drizzle-orm/node-postgres";
import pg from "pg";
import { questionnaires, questions, submissions, submissionAnswers } from './schema/questionnaires.ts';

const schema = {
  questionnaires,
  questions,
  submissions,
  submissionAnswers,
};

const { Pool } = pg;
const pool = new Pool({
  connectionString: process.env.DATABASE_URL!
});

export const db = drizzle({ client: pool, schema });