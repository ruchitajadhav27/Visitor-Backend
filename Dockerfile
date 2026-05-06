# Stage 1: Build the application
FROM openjdk:17-jdk-slim AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Stage 2: Run the application
FROM openjdk:17-jdk-slim
WORKDIR /app
# Copy only the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# Use a specific filename to avoid "multiple matches" errors
ENTRYPOINT ["java", "-jar", "app.jar"]