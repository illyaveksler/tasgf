import { Resource } from "sst";
import { drizzle } from "drizzle-orm/aws-data-api/pg";
import { RDSDataClient } from "@aws-sdk/client-rds-data";

export const db = drizzle(new RDSDataClient({}), {
  database: Resource.MyDatabase.database,
  secretArn: Resource.MyDatabase.secretArn,
  resourceArn: Resource.MyDatabase.clusterArn
});
