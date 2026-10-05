# ====================================================================
# EXPORTRACE BACKEND - MULTI-STAGE DOCKERFILE (JAVA 21 / SPRING BOOT)
# ====================================================================

# Step 1: Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# Step 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create directory for SQLite persistence in containers/volumes if mounted
RUN mkdir -p /app/data

# Copy built JAR artifact from build stage
COPY --from=build /build/target/*.jar app.jar

# Expose default HTTP Port (Render sets $PORT dynamically)
ENV PORT=8080
EXPOSE 8080

# Run Spring Boot Application
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
