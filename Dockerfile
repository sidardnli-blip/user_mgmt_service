# ---------- Stage 1: Build ----------
FROM gradle:jdk25-alpine AS build
WORKDIR /app

# 1. Nur die Gradle-Konfigurationsdateien kopieren -> Dependency-Layer cachen
COPY settings.gradle build.gradle ./
RUN gradle dependencies --no-daemon || true

# 2. Jetzt erst den Quellcode kopieren und die JAR bauen
COPY src ./src
RUN gradle bootJar --no-daemon -x test

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

# Non-Root User anlegen
RUN addgroup -S spring && adduser -S spring -G spring

# Nur die fertige JAR aus der Build-Stage übernehmen
COPY --from=build /app/build/libs/user-mgmt-service-0.0.1-SNAPSHOT.jar app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]