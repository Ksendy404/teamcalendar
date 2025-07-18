# Этап сборки
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Этап запуска
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Копируем собранный jar-файл
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Запуск приложения
CMD ["java", "-jar", "app.jar"]