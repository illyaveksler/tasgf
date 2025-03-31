import { defineConfig } from "drizzle-kit";
import { Resource } from "sst";

const DATABASE_URL = `postgresql://${Resource.MyPostgres.username}:${Resource.MyPostgres.password}@${Resource.MyPostgres.host}:${Resource.MyPostgres.port}/${Resource.MyPostgres.database}`


export default defineConfig({
  dialect: 'postgresql',
  schema: './src/db/schema',
  dbCredentials: {
    url: DATABASE_URL
  },
})
