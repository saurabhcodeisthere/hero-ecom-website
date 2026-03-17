# Base image with Java 17
FROM eclipse-temurin:17-jdk-ubi9-minimal

# Set working directory inside container
WORKDIR /app

# Copy the built jar into container
COPY target/*.jar app.jar

# Expose Eureka server port
EXPOSE 8761

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
