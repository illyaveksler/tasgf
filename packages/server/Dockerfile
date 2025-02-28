FROM node:23-alpine

WORKDIR /app/

COPY package.json /app
RUN npm install

COPY . /app

ENTRYPOINT ["node", "--experimental-transform-types", "src/index.ts"]