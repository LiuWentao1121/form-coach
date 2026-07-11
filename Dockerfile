# Build stage: compile the Spring Boot JAR with Maven + JDK 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# Runtime stage: run the built JAR on the JRE
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/target/form-coach-1.0.0.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
