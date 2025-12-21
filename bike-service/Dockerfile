# 1. Base image with Java 17 (recommended for Spring Boot)
FROM eclipse-temurin:17-jdk-ubi9-minimal

# 2. Set working directory inside container
WORKDIR /app

# 3. Copy the built jar into container
COPY target/*.jar app.jar

# 4. Expose application port
EXPOSE 8081

# 5. Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
