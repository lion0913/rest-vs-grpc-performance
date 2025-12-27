# Build stage
FROM gradle:8.11.1-jdk21 AS build

WORKDIR /app

# Copy gradle files
COPY build.gradle settings.gradle ./
COPY src ./src

# Build the application
RUN gradle clean build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose ports
EXPOSE 8081 9091

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]