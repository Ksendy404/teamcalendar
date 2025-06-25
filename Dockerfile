# Этап сборки
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Этап запуска
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Установка logrotate и cron
RUN apt-get update && \
    apt-get install -y logrotate cron && \
    apt-get clean

# Копируем собранный jar-файл
COPY --from=build /app/target/*.jar app.jar

# Копируем logrotate конфиг и скрипт запуска
COPY logrotate.conf /etc/logrotate.d/teamcalendar
COPY start.sh /start.sh
RUN chmod +x /start.sh && chmod 644 /etc/logrotate.d/teamcalendar

# Настраиваем cron для запуска очистки логов
RUN echo "0 0 * * * /usr/sbin/logrotate /etc/logrotate.d/teamcalendar >> /var/log/logrotate.log 2>&1" > /etc/cron.d/logrotate-job && \
    chmod 0644 /etc/cron.d/logrotate-job && \
    crontab /etc/cron.d/logrotate-job

EXPOSE 8080

# Запуск cron + app
CMD ["/start.sh"]