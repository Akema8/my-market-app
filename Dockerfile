FROM eclipse-temurin:21
WORKDIR /app
COPY market-web/target/market-web-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
EXPOSE 8080