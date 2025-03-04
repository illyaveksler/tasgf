FROM node:23-bullseye-slim

WORKDIR /app/

COPY ./ /app

RUN npm install

ENTRYPOINT ["node", "--experimental-transform-types", "./src/index.ts"]
