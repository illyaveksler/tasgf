import { migrate } from "drizzle-orm/postgres-js/migrator";
import { db } from "./index.ts";

export const handler = async () => {
  await migrate(db, {
    migrationsFolder: "./migrations",
  });
};