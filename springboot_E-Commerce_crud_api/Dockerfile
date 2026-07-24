# ================================
# Stage 1: Build the Application
# ================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom.xml first to leverage Docker layer caching for dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the rest of the source code and build the JAR
COPY src ./src
RUN mvn clean package -DskipTests

# ================================
# Stage 2: Runtime Environment
# ================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user for security best practices
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the application port (matching server.port=8888 in your properties)
EXPOSE 8888

# Set active environment variables if needed
ENV JAVA_OPTS=""

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]