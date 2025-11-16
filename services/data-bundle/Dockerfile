# Dockerfile for SERVICE_NAME
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the JAR file (built separately)
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
