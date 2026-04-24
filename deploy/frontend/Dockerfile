FROM node:22-bookworm

WORKDIR /workspace

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

EXPOSE 3000

CMD ["sh", "-c", "npm install --include=dev && npm run dev -- --host 0.0.0.0"]
