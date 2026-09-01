# syntax=docker/dockerfile:1

# Build stage. Tests are skipped here and the client generator is off: `./up.sh` runs
# `mvn verify` on the host, which is what produces the OpenAPI spec and generates the
# Angular client into frontend/src/app/api. This image only needs the jar.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

COPY pom.xml checkstyle.xml checkstyle-suppressions.xml ./
COPY src ./src

# No ~/.m2/settings.xml inside the image, so Maven resolves straight from Central
# and the project-local .mvn/settings.xml is not needed here.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -DskipTests -Dopenapi.generate.skip=true package

FROM eclipse-temurin:25-jre
WORKDIR /app

# curl is here for the container healthcheck, nothing else.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --uid 10001 dashboard

COPY --from=build /build/target/program-dashboard-backend-*.jar /app/app.jar

USER dashboard
ENV TZ=UTC
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
