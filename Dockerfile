# -------- Stage 1: Build the application --------
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy pom first (for caching dependencies)
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build the jar
RUN mvn clean package -DskipTests


# -------- Stage 2: Run the application --------
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copy jar from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]