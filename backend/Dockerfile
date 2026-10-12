# syntax=docker/dockerfile:1

FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp package -DskipTests && \
    cp target/open-circle-api-*.jar application.jar

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN groupadd --system --gid 10001 opencircle && \
    useradd --system --uid 10001 --gid opencircle --no-create-home opencircle

WORKDIR /app

COPY --from=build --chown=opencircle:opencircle /workspace/application.jar ./application.jar

USER opencircle:opencircle

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java -Dserver.port=${PORT:-8080} -jar /app/application.jar"]
