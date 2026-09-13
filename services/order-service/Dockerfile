# Build context is the repo root (see docker-compose.yml), because this is a
# multi-module Maven reactor build: order-service depends on the `common` module.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY services/common services/common
COPY services/order-service services/order-service
RUN mvn -q -pl services/common,services/order-service -am -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/services/order-service/target/order-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
