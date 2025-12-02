# ============================
# 1) Build stage (Maven + JDK)
# ============================
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy entire source to container
COPY . .

# Clean + build (skip tests)
RUN mvn clean install -DskipTests


# ============================
# 2) Runtime stage
# ============================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy jar from stage build
COPY --from=builder /app/target/orchestrator*.jar app.jar

# ENTRYPOINT
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
