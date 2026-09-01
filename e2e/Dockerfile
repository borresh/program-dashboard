FROM mcr.microsoft.com/playwright:v1.56.0-noble

WORKDIR /e2e

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

CMD ["npx", "playwright", "test"]
