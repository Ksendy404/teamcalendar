# Используем официальный OpenJDK образ
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/*.jar app.jar

RUN mkdir -p /app/config

RUN addgroup --system spring && adduser --system spring --ingroup spring
RUN chown -R spring:spring /app
USER spring

EXPOSE 8080

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "/app/app.jar"]