/// <reference path="./.sst/platform/config.d.ts" />

export default $config({
  app(input) {
    return {
      name: "tasgf",
      removal: input?.stage === "production" ? "retain" : "remove",
      protect: ["production"].includes(input?.stage),
      home: "aws",
    };
  },
  async run() {
    const vpc = new sst.aws.Vpc("MyVpc", { bastion: true });
    const database = new sst.aws.Aurora("MyDatabase", {
      engine: "postgres",
      dataApi: true,
      vpc
    });
    const cluster = new sst.aws.Cluster("MyCluster", { vpc });
  
    new sst.aws.Service("MyService", {
      link: [database],
      cluster,
      loadBalancer: {
        ports: [{ listen: "80/http" }],
      },
      image: {
        context: "./packages/server"
      },
      dev: {
        command: "node --experimental-transform-types --watch ./src/index.ts",
      },
    });

    new sst.x.DevCommand("Studio", {
      link: [database],
      dev: {
        command: "npx drizzle-kit studio",
        directory: "./packages/server"
      },
    });

    new sst.x.DevCommand("Migrate", {
      link: [database],
      dev: {
        command: "npx drizzle-kit push",
        directory: "./packages/server",
        autostart: false
      },
    });
  },
});
