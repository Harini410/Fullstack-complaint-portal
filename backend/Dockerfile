# Multi-Stage Dockerfile for Spring Boot Backend
# Stage 1: Build stage
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app

COPY pom.xml .
COPY . .

# If jar is already prebuilt, use it; otherwise build using maven
RUN if [ -f "./target/complaint-backend-0.0.1-SNAPSHOT.jar" ]; then \
      echo "Prebuilt backend JAR detected. Skipping Maven rebuild."; \
    else \
      mvn clean package -DskipTests -B; \
    fi

# Stage 2: Minimal hardened runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Security: Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy executable jar from builder stage
COPY --from=builder /app/target/complaint-backend-0.0.1-SNAPSHOT.jar app.jar

# Expose REST API port
EXPOSE 8080

# Configure production JVM flags
ENV JAVA_OPTS="-Xms128m -Xmx384m -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
