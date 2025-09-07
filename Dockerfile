FROM eclipse-temurin:23-jdk-alpine

WORKDIR /app

COPY target/netology-diplom-0.0.1-SNAPSHOT.jar app.jar

RUN mkdir -p /app/storage

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]