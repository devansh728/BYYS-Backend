# Build stage (Java 21 + Gradle)
FROM eclipse-temurin:21-jdk-jammy as builder

WORKDIR /app

# Copy files and make gradlew executable
COPY . .
RUN chmod +x gradlew  # ← THIS IS CRUCIAL

# Build (skip tests)
RUN ./gradlew clean build -x test

# Runtime stage (Lightweight JRE)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy built JAR
COPY --from=builder /app/build/libs/*.jar app.jar

# Environment variables
ENV PORT=8080
EXPOSE $PORT

# Run the app
CMD ["java", "-jar", "app.jar", "--server.port=${PORT}"]