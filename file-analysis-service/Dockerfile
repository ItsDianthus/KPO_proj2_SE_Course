FROM ubuntu:latest
LABEL authors="Dianazavr"

WORKDIR /app

COPY target/file-analysis-service-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]