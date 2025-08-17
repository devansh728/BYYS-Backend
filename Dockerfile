# Use OpenJDK 21 for building
FROM eclipse-temurin:21-jdk-jammy as builder

WORKDIR /app

# Copy Gradle files first (for caching)
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .
COPY src ./src

# Build the app (skip tests for faster builds)
RUN ./gradlew clean build -x test

# Final lightweight image (only JRE)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the built JAR
COPY --from=builder /app/build/libs/*.jar app.jar

# Set default port (Railway/Render will override)
ENV PORT=8080
EXPOSE $PORT

# Run the app
CMD ["sh", "-c", "java -jar app.jar --server.port=${PORT}"]