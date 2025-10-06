FROM eclipse-temurin:21-jre-alpine
COPY target/orchestrator*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
