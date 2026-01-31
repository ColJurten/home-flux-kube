FROM eclipse-temurin:17-jdk-alpine as build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/dummy-java-loggen-1.0-SNAPSHOT.jar app.jar
CMD ["java", "-jar", "app.jar"]
