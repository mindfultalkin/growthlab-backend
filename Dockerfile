# Multi-stage build for production-ready Spring Boot application 
FROM eclipse-temurin:17-jdk-jammy AS builder

# Set working directory
WORKDIR /opt/app

# Install curl for health checks (needed for the final stage)
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy Maven wrapper and pom.xml first (for better layer caching)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Make mvnw executable
RUN chmod +x ./mvnw

# Download dependencies with retry mechanism
RUN ./mvnw dependency:go-offline -B || \
    ./mvnw dependency:go-offline -B || \
    ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application with proper clean
RUN ./mvnw clean package -DskipTests -U

# Production stage with minimal runtime image
FROM eclipse-temurin:17-jre-jammy

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -r springboot && useradd -r -g springboot springboot

# Set working directory
WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /opt/app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R springboot:springboot /app

# Switch to non-root user
USER springboot

# Expose port
EXPOSE 8080

# Set JVM options for production
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]