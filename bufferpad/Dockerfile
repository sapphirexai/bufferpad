FROM node:18-bullseye-slim AS build

WORKDIR /app

# Webpack 3 uses legacy OpenSSL algorithms on modern Node.js.
ENV NODE_OPTIONS=--openssl-legacy-provider

COPY package*.json ./
RUN npm ci --legacy-peer-deps

COPY . .
RUN npm run build

FROM nginx:1.22.1-alpine

COPY default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/ /usr/share/nginx/bufferpad/

EXPOSE 80
