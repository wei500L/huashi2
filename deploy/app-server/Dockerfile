FROM eclipse-temurin:25-jdk@sha256:c42fecf62f32725c65cfea284c012526d6fb31cc78123c740ebdc1cfd2dced12 AS builder
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw mvnw
COPY pom.xml pom.xml
COPY shared-kernel/pom.xml shared-kernel/pom.xml
COPY app-server/pom.xml app-server/pom.xml
COPY ai-gateway/pom.xml ai-gateway/pom.xml
RUN sed -i 's/\r$//' mvnw \
    && chmod +x mvnw \
    && ./mvnw -pl shared-kernel,app-server -am -q dependency:go-offline

COPY shared-kernel/src shared-kernel/src
COPY app-server/src app-server/src
RUN ./mvnw -pl app-server -am -q clean package -Dmaven.test.skip=true

FROM eclipse-temurin:25-jdk@sha256:c42fecf62f32725c65cfea284c012526d6fb31cc78123c740ebdc1cfd2dced12
WORKDIR /app
RUN useradd --system --create-home --shell /usr/sbin/nologin appuser \
    && mkdir -p /app/logs \
    && chown -R appuser:appuser /app
COPY --from=builder /workspace/app-server/target/app-server-0.1.0-SNAPSHOT.jar /app/app.jar
RUN chown appuser:appuser /app/app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
