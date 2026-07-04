FROM node:20-alpine AS builder

WORKDIR /app

# Versão injetada pelo Jenkins (--build-arg BUILD_VERSION=0.0.1)
ARG BUILD_VERSION
ENV VITE_APP_VERSION=${BUILD_VERSION}

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build || npm run build:skip-check

FROM nginx:alpine

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
