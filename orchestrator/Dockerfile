FROM eclipse-temurin:21-jre-alpine
COPY target/orchestrator*.jar app.jar
EXPOSE 9001
ENTRYPOINT ["java", "-jar", "/app.jar"]
