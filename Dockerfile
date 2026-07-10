FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/form-coach-1.0.0.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
