# Use Eclipse Temurin JDK 17 (stable & supported)
FROM eclipse-temurin:17-jdk-jammy

# Set working directory
WORKDIR /app

# Copy build files
COPY target/*.jar app.jar

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]
