# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY mvnw .mvn/wrapper/maven-wrapper.properties .
COPY .mvn .mvn
COPY pom.xml .
COPY src ./src
# package tự download dependencies khi cần — go-offline hay bị timeout mạng
RUN chmod +x ./mvnw && ./mvnw package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring \
    && mkdir -p /app/logs && chown -R spring:spring /app
COPY --from=builder /app/target/BackEnd-0.0.1-SNAPSHOT.jar app.jar
USER spring
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=docker
ENTRYPOINT ["java", "-jar", "app.jar"]
