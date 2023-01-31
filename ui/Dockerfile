# syntax=docker/dockerfile:1

# This Dockerfile performs a multi-stage build and must be built with BuildKit: `docker buildx build .`.
# By default, it will produce an optimized image with the production build, served as static content with nginx.
# Target the development stage to produce a larger dev image running Vite dev server. Good for Tilt live reload.
FROM node:18-alpine as sources

WORKDIR /app

# Dependencies
COPY --link package.json yarn.lock ./
RUN yarn install --pure-lockfile

# Sources
COPY --link . ./

# Starts Vite dev server, used by Tilt
FROM sources as development

EXPOSE 8080
ENTRYPOINT ["yarn", "run", "dev"]
CMD ["--port", "8080"]

# Builds the app for production
FROM sources as build

ARG VITE_BASE_URL
ARG VITE_KEYCLOAK_URL
ARG VITE_KEYCLOAK_REALM
ARG VITE_KEYCLOAK_CLIENT_ID

RUN ["yarn", "run", "build"]

# Serves prod build with nginx
FROM nginx:1.23.1-alpine as production

# Configure nginx
COPY --link /nginx /etc/nginx

# Copy prod files from prod-build stage
COPY --from=build --link /app/dist /usr/share/nginx/html

EXPOSE 8080
ENTRYPOINT ["nginx", "-g", "daemon off;"]
