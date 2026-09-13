# Build context is the repo root (see docker-compose.yml), because this is a
# multi-module Maven reactor build: api-gateway depends on the `common` module.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY services/common services/common
COPY services/api-gateway services/api-gateway
RUN mvn -q -pl services/common,services/api-gateway -am -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/services/api-gateway/target/api-gateway-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
