# Multi-stage build for React frontend and Spring Boot backend
FROM node:20 AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

FROM maven:3.9.6-eclipse-temurin-17 AS backend-build
WORKDIR /app
COPY pom.xml ./
COPY src/ ./src/
# Copy built static assets from frontend stage to spring boot resources
COPY --from=frontend-build /app/frontend/dist/ ./src/main/resources/static/
# Run Maven package skipping tests using global maven
RUN mvn clean package -DskipTests

# Final minimal JRE run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=backend-build /app/target/parking-lot-1.0-SNAPSHOT-exec.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
