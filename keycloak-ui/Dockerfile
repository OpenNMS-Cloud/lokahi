# syntax=docker/dockerfile:1

# This Dockerfile performs a multi-stage build and must be built with BuildKit: `docker buildx build .`.
# By default, it will produce an optimized image with the production build.
# Target the development stage to produce an unoptimized dev image running start-dev.

FROM quay.io/keycloak/keycloak:19.0.2 as build

ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_FEATURES=token-exchange
ENV KC_DB=postgres
ENV KC_HTTP_RELATIVE_PATH=/auth
ENV KC_CACHE=ispn
ENV KC_CACHE_STACK=kubernetes

WORKDIR /opt/keycloak

# Install custom providers
RUN curl -sSfL https://github.com/aerogear/keycloak-metrics-spi/releases/download/2.5.3/keycloak-metrics-spi-2.5.3.jar -o /opt/keycloak/providers/keycloak-metrics-spi-2.5.3.jar
RUN mkdir -p /opt/keycloak/agent
RUN curl -sSfL https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.25.0/opentelemetry-javaagent.jar -o /opt/keycloak/agent/opentelemetry-javaagent.jar

COPY --link --chown=1000 themes themes

RUN /opt/keycloak/bin/kc.sh build

FROM build as development

USER 0
RUN microdnf install tar # allows for Tilt live reload
USER 1000

ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start-dev"]

FROM quay.io/keycloak/keycloak:19.0.2 as production

COPY --link --from=build /opt/keycloak/ /opt/keycloak/

ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start", "--optimized"]
