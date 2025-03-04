/// <reference path="./.sst/platform/config.d.ts" />

export default $config({
  app(input) {
    return {
      name: "tasgf",
      removal: "remove",
      protect: false,
      home: "aws",
    };
  },
  async run() {
    const vpc = new sst.aws.Vpc("MyVpc", { bastion: true, nat: "managed" });
    const rds = new sst.aws.Postgres("MyPostgres", { vpc });
  
    const DATABASE_URL = $interpolate`postgresql://${rds.username}:${rds.password}@${rds.host}:${rds.port}/${rds.database}`;

    const cluster = new sst.aws.Cluster("MyCluster", { vpc });
  
    new sst.aws.Service("MyService", {
      cluster,
      link: [rds],
      environment: { DATABASE_URL },
      loadBalancer: {
        ports: [{ listen: "80/http" }],
      },
      dev: {
        command: "node --experimental-transform-types --watch ./src/index.ts",
      },
    });

    const migrator = new sst.aws.Function("DatabaseMigrator", {
      handler: "src/db/migrator.handler",
      link: [rds],
      environment: { DATABASE_URL },
      vpc,
      copyFiles: [
        {
          from: "drizzle",
          to: "./migrations",
        },
      ],
    });
    
    if (!$dev) {
      new aws.lambda.Invocation("DatabaseMigratorInvocation", {
        input: Date.now().toString(),
        functionName: migrator.name,
      });
    }

    new sst.x.DevCommand("Drizzle", {
      link: [rds],
      environment: { DATABASE_URL },
      dev: {
        autostart: false,
        command: "npx drizzle-kit studio",
      },
    });
  },
});
