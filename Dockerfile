# syntax=docker/dockerfile:1
FROM maven:3.9.13-eclipse-temurin-21-noble AS build
WORKDIR /workspace
ARG MODULE

COPY pom.xml ./
COPY ${MODULE}/pom.xml ${MODULE}/pom.xml
COPY ${MODULE}/src ${MODULE}/src

RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -f "${MODULE}/pom.xml" -DskipTests package \
    && cp "${MODULE}"/target/*.jar /gamehub.jar

FROM eclipse-temurin:21-jre-noble
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 gamehub \
    && useradd --uid 10001 --gid gamehub --no-create-home gamehub \
    && install -d -o gamehub -g gamehub /app/data

WORKDIR /app
COPY --from=build /gamehub.jar ./gamehub.jar
USER gamehub
ENTRYPOINT ["java", "-jar", "/app/gamehub.jar"]
