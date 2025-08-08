# Multi-stage Dockerfile for URL Shortener Spring Boot Application
FROM openjdk:17-jdk-slim as builder

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml first for better layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies (this layer will be cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests -B

# Runtime stage
FROM openjdk:17-jdk-slim

# Install necessary packages and create app user for security
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        curl \
        netcat-traditional && \
    rm -rf /var/lib/apt/lists/* && \
    addgroup --system appgroup && \
    adduser --system --group appuser

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create logs directory and set permissions
RUN mkdir -p /app/logs && \
    chown -R appuser:appgroup /app

# Health check script
COPY --chown=appuser:appgroup <<EOF /app/healthcheck.sh
#!/bin/bash
curl -f http://localhost:8080/api/v1/health || exit 1
EOF

RUN chmod +x /app/healthcheck.sh

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Environment variables with defaults
ENV SPRING_PROFILES_ACTIVE=docker
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV TZ=UTC

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD /app/healthcheck.sh

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]