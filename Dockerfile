# 🏗️ Multi-Stage Dockerfile for Spring Boot Application
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml first for better layer caching
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd pom.xml ./

# Make Maven wrapper executable
RUN chmod +x mvnw

# Download dependencies (cached layer if pom.xml unchanged)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application (skip tests for faster builds)
RUN ./mvnw clean package -DskipTests -B

# Extract JAR layers for better caching
RUN java -Djarmode=layertools -jar target/*.jar extract

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime

# Install dumb-init and wget for proper signal handling and health checks
RUN apk add --no-cache dumb-init wget

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

# Copy extracted layers from builder stage
COPY --from=builder --chown=appuser:appgroup /app/dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /app/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appgroup /app/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /app/application/ ./

# Switch to non-root user
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Use dumb-init for proper signal handling
ENTRYPOINT ["dumb-init", "--"]

# Run application with optimized JVM settings
CMD ["java", \
     "-XX:+UseContainerSupport", \
     "-XX:MaxRAMPercentage=75.0", \
     "-XX:+UseG1GC", \
     "-XX:+UseStringDeduplication", \
     "-Djava.security.egd=file:/dev/./urandom", \
     "-Dspring.profiles.active=k8s", \
     "org.springframework.boot.loader.launch.JarLauncher"]