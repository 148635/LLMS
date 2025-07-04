# Stage 1: Build the Spring Boot application (using a multi-stage build for smaller final image)
FROM eclipse-temurin:17-jdk-focal as build
# If you are using a different Java version, change '17' accordingly.
# 'focal' is a good choice for a stable Ubuntu base.

WORKDIR /app

COPY pom.xml ./
COPY mvnw ./
COPY .mvn ./.mvn
COPY src ./src

# Make the mvnw script executable
RUN chmod +x mvnw
# If you don't have Maven Wrapper (mvnw), use: RUN mvn clean package -DskipTests

# For Gradle (uncomment these if using Gradle):
# RUN ./gradlew bootJar -x test
RUN ./mvnw clean package -DskipTests
# Extract the application JAR path
# For Maven:
ARG JAR_FILE=target/*.jar
# For Gradle (uncomment these if using Gradle):
# ARG JAR_FILE=build/libs/*.jar

# Stage 2: Create the final lightweight runtime image
FROM eclipse-temurin:17-jre-focal
# Use JRE for smaller image size. Keep the Java version consistent with build stage.

WORKDIR /app

# Copy the built JAR from the 'build' stage
COPY --from=build /app/${JAR_FILE} app.jar

# Expose the port your Spring Boot app listens on (default is 8080)
EXPOSE 8080

# Command to run the Spring Boot application
CMD ["java", "-jar", "app.jar"]