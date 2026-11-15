FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./

RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY src src

RUN ./mvnw clean verify -Dtest='!ServerApplicationTest,!StatisticsEventRepositoryTest,!MongoPersistenceTest,!ActivityParticipationControllerTest,!MongoIndexTest'

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl

# OpenTelemetry Java agent. It is only loaded when JAVA_TOOL_OPTIONS points to it, as
# docker-compose.yaml does locally; Cloud Run does not, so the deployed backend runs without it.
ARG OTEL_AGENT_VERSION=2.32.0
ARG OTEL_AGENT_SHA256=f787eb6c7f3d18e69a431e108a15278d25ee37f83d68b678f621e063f3988f82
RUN mkdir -p /otel \
  && curl -fsSL -o /otel/opentelemetry-javaagent.jar \
    "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar" \
  && echo "${OTEL_AGENT_SHA256}  /otel/opentelemetry-javaagent.jar" | sha256sum -c -

COPY --from=builder /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
