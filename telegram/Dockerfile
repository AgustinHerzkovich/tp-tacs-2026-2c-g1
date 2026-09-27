FROM node:20-alpine AS builder

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY tsconfig.json ./
COPY src ./src

RUN npm run build

# Local development: recompiles and restarts the function on every change under src/.
FROM builder AS dev

EXPOSE 8080

CMD ["npm", "run", "dev"]

FROM node:20-alpine

WORKDIR /app
ENV NODE_ENV=production

COPY package.json package-lock.json ./
RUN npm ci --omit=dev

COPY --from=builder /app/dist ./dist

EXPOSE 8080

CMD ["npx", "functions-framework", "--target=webhook", "--source=dist", "--port=8080"]
