# ==============================================================================
# HeliShop Core E-Commerce Backend - Multi-stage Dockerfile
# Stage 1: Build JAR using Maven with Eclipse Temurin 21 Alpine
# Stage 2: Runtime image using Eclipse Temurin 21 JRE Alpine (Ultra-lightweight & Secure)
# ==============================================================================

# Stage 1: Builder
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy dependency definition first for Docker layer caching
COPY pom.xml .

# Download dependencies offline to optimize rebuilds
RUN mvn dependency:go-offline -B

# Copy source code and build executable JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==============================================================================
# Stage 2: Runtime Environment
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS runner

# Create non-root system group and user for security best practices
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy built jar from builder stage
COPY --from=builder /build/target/helishop-core-*.jar app.jar

# Adjust ownership
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose standard Spring Boot HTTP port
EXPOSE 8080

# Environment variables for JVM container optimization
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom" \
    SPRING_PROFILES_ACTIVE="prod"

# Healthcheck for container orchestration
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/v3/api-docs || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
