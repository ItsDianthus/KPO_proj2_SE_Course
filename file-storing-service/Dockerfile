FROM ubuntu:latest
LABEL authors="Dianazavr"

WORKDIR /app

COPY target/file-storing-service-*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]