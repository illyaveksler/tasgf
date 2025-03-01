import { Resource } from "sst";
import { defineConfig } from "drizzle-kit";

export default defineConfig({
  dialect: 'postgresql',
  schema: './src/db/schema',
  dbCredentials: {
    host: Resource.MyDatabase.host,
    port: Resource.MyDatabase.port,
    user: Resource.MyDatabase.username,
    password: Resource.MyDatabase.password,
    database: Resource.MyDatabase.database,
    ssl: { rejectUnauthorized: false },
  },
})
