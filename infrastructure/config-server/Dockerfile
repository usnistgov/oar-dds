# Dockerfile for Config Server
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the JAR file (built separately)
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8888

# Explicitly set native profile to avoid git backend issues
ENTRYPOINT ["java", "-Dspring.profiles.active=native", "-jar", "app.jar"]
