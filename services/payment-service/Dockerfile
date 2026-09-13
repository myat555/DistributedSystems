# Build context is the repo root (see docker-compose.yml), because this is a
# multi-module Maven reactor build: payment-service depends on the `common` module.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY services/common services/common
COPY services/payment-service services/payment-service
RUN mvn -q -pl services/common,services/payment-service -am -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/services/payment-service/target/payment-service-*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
