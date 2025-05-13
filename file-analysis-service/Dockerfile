FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/file-analysis.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]