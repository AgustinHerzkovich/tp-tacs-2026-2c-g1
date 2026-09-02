FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY src src

RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]

# ahora para correr los tests tengo que hacer:
# docker compose up -d mongodb
  #./mvnw clean verify -Dspring.mongodb.uri="mongodb://admin:password123@localhost:27017/mi_base_de_datos?authSource=admin"
