# Build context is the repo root (see docker-compose.yml), because this is a
# multi-module Maven reactor build: job-queue-service depends on the `common` module.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY services/common services/common
COPY services/job-queue-service services/job-queue-service
RUN mvn -q -pl services/common,services/job-queue-service -am -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/services/job-queue-service/target/job-queue-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
