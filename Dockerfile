# Stage 1: Build the Spring Boot application (using a multi-stage build for smaller final image)
FROM eclipse-temurin:17-jdk-focal as build
# If you are using a different Java version, change '17' accordingly.
# 'focal' is a good choice for a stable Ubuntu base.

# Expose the port your Spring Boot app listens on (default is 8080)
EXPOSE 8080

WORKDIR /app

# Copy the jar file into the container
COPY target/model-0.0.1-SNAPSHOT.jar.jar app.jar

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]