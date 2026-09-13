# Build context is the repo root (see docker-compose.yml), because this is a
# multi-module Maven reactor build: discovery-server depends on the `common` module.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY services/common services/common
COPY services/discovery-server services/discovery-server
RUN mvn -q -pl services/common,services/discovery-server -am -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/services/discovery-server/target/discovery-server-*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
