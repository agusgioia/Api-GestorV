FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY . /app

RUN chmod +x ./mvnw

RUN ./mvnw clean install -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "target/app-trello-0.0.1-SNAPSHOT.jar"]
