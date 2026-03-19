FROM maven:3.9.11-eclipse-temurin-25 AS builder
WORKDIR /workspace

COPY pom.xml pom.xml
COPY shared-kernel/pom.xml shared-kernel/pom.xml
COPY ai-gateway/pom.xml ai-gateway/pom.xml
COPY app-server/pom.xml app-server/pom.xml
RUN mvn -pl shared-kernel,ai-gateway -am -q dependency:go-offline

COPY shared-kernel/src shared-kernel/src
COPY ai-gateway/src ai-gateway/src
RUN mvn -pl ai-gateway -am -q clean package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=builder /workspace/ai-gateway/target/ai-gateway-0.1.0-SNAPSHOT.jar /app/app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
