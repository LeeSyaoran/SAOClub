FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/BackEnd-0.0.1-SNAPSHOT.jar app.jar
RUN addgroup -S spring && adduser -S spring -G spring \
    && mkdir -p /app/logs && chown -R spring:spring /app
USER spring
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=docker
ENTRYPOINT ["java", "-jar", "app.jar"]
