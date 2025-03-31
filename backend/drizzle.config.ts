import { defineConfig } from "drizzle-kit";

if (!process.env.DATABASE_URL) {
  throw new Error('Missing envrionment variable DATABASE_URL')
}

export default defineConfig({
  dialect: 'postgresql',
  schema: './src/db/schema',
  dbCredentials: {
    url: process.env.DATABASE_URL,
    // ssl: { rejectUnauthorized: false },
  },
})
