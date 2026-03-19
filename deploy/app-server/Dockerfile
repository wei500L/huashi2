FROM maven:3.9.11-eclipse-temurin-25 AS builder
WORKDIR /workspace

COPY pom.xml pom.xml
COPY shared-kernel/pom.xml shared-kernel/pom.xml
COPY app-server/pom.xml app-server/pom.xml
COPY ai-gateway/pom.xml ai-gateway/pom.xml
RUN mvn -pl shared-kernel,app-server -am -q dependency:go-offline

COPY shared-kernel/src shared-kernel/src
COPY app-server/src app-server/src
RUN mvn -pl app-server -am -q clean package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=builder /workspace/app-server/target/app-server-0.1.0-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
