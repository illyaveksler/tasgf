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
    const vpc = new sst.aws.Vpc("MyVpc");
    const cluster = new sst.aws.Cluster("MyCluster", { vpc });
  
    new sst.aws.Service("MyService", {
      cluster,
      loadBalancer: {
        ports: [{ listen: "80/http" }],
      },
      image: {
        // dockerfile: "packages/server/Dockerfile",
        context: "packages/server"
      },
      dev: {
        command: "node --experimental-transform-types --watch packages/server/src/index.ts",
      },
    });
  },
});
