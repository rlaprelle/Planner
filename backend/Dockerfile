# syntax=docker/dockerfile:1.7

# ---- Builder stage: produce the Spring Boot fat jar ----
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Cache dependencies in their own layer
COPY pom.xml ./
RUN mvn -B -e -ntp dependency:go-offline

# Build the application
COPY src ./src
RUN mvn -B -e -ntp -DskipTests package

# Split the fat jar into layers for better runtime caching
RUN mkdir -p /build/extracted \
    && cp /build/target/*.jar /build/extracted/app.jar \
    && cd /build/extracted \
    && java -Djarmode=layertools -jar app.jar extract \
    && rm app.jar

# ---- Runtime stage: minimal JRE, non-root ----
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Run as a dedicated non-root user
RUN addgroup -S app && adduser -S app -G app

# Copy layered jar contents — ordered least-to-most volatile so code-only
# rebuilds reuse the dependency and loader layers from the cache.
COPY --from=builder --chown=app:app /build/extracted/dependencies/ ./
COPY --from=builder --chown=app:app /build/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=app:app /build/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=app:app /build/extracted/application/ ./

USER app

EXPOSE 8080

# Spring Boot 3.2 moved the launcher to org.springframework.boot.loader.launch
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
