# ──────────────────────────────────────────────
# Stage 1: Build with Maven
# ──────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy POM first for layer caching of dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source code and build
COPY src src
RUN mvn clean package -DskipTests -q

# ──────────────────────────────────────────────
# Stage 2: Lean Runtime Image
# ──────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="GameSphere Team"
LABEL version="1.0.0"
LABEL description="GameSphere – Esports Tournament Management Platform"

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S gamesphere && adduser -S gamesphere -G gamesphere

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set ownership
RUN chown gamesphere:gamesphere app.jar

USER gamesphere

EXPOSE 8080

# Container health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/api/v1/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
