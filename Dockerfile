# Stage 1: Build the Spring Boot application (using a multi-stage build for smaller final image)
FROM eclipse-temurin:17-jdk-focal as build

EXPOSE 8080

WORKDIR /app

RUN ./mvnw clean package -DskipTests

RUN ls target/
# Copy the jar file into the container
COPY target/model-0.0.1-SNAPSHOT.jar.jar app.jar

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]