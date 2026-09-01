# syntax=docker/dockerfile:1

FROM node:24-alpine AS build
WORKDIR /build

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:1.29-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist/frontend/browser /usr/share/nginx/html

# The nginx image runs every executable in /docker-entrypoint.d before starting.
# This one turns the environment into config.json, which is how a static bundle
# gets runtime configuration without being rebuilt.
COPY docker-entrypoint.d/40-write-config-json.sh /docker-entrypoint.d/40-write-config-json.sh
RUN chmod +x /docker-entrypoint.d/40-write-config-json.sh

EXPOSE 4200
