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
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=builder /app/target/BackEnd-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /app/logs /app/uploads && chown -R spring:spring /app
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=docker
# ponytail: volume mount vào /app/uploads có thể tạo thư mục với owner mới.
# chmod u+w đủ cho spring user ghi file; chown xử lý trường hợp volume chưa tồn tại.
ENTRYPOINT ["sh", "-c", "chmod -R u+w /app/uploads 2>/dev/null; exec java -jar app.jar"]
USER spring
