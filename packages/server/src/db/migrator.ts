import { migrate } from "drizzle-orm/postgres-js/migrator";
import { db } from "./index.ts";

export const handler = async (event: any) => {
  await migrate(db, {
    migrationsFolder: "./migrations",
  });
};