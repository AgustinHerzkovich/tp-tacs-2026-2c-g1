FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY src src

# ServerApplicationTest uses Testcontainers (MongoDB container), which cannot run
# inside the build image because it has no access to the Docker daemon.
RUN ./mvnw clean verify -Dtest='!ServerApplicationTest'

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
