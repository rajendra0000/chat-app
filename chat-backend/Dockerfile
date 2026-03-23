# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
# Cache dependencies layer separately for faster rebuilds
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080

# -Xmx400m: Render free tier has 512MB RAM total; leave headroom for OS/JVM overhead
# -XX:+UseContainerSupport: respect Docker memory limits
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-Xmx400m", "-XX:+UseContainerSupport", "-jar", "app.jar"]
