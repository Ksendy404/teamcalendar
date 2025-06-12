Telegram-бот для уведомлений о событиях календаря, который интегрируется с Yandex CalDAV.

Для использования добавьте файл .env с пропертями

```
TELEGRAM_BOT_TOKEN=
TELEGRAM_BOT_USERNAME=
TELEGRAM_BOT_ADMIN_CHAT_ID=
TELEGRAM_BOT_NOTIFICATION_CHAT_ID=
TELEGRAM_BOT_ERROR_CHAT_ID=
CALDAV_URL= Ссылка на caldav yandex для вашей почты 
CALDAV_USERNAME= почта
CALDAV_PASS= пароль
```

Для запуска локально используйте 
```
set -a && source .env && set +a && mvn spring-boot:run
```

Для запуска
```
# Создайте директорию
mkdir -p /opt/telegram-bot
cd /opt/telegram-bot
```
```
# Создайте .env с реальными данными
nano .env
```
```
# Ограничьте доступ только для root
chmod 600 .env
chown root:root .env
```
```
# Docker Compose  
docker-compose up --build -d
```
```
# Проверьте логи
docker-compose logs -f telegram-bot
```
```
# Проверьте статус
docker-compose ps
```



1. Проверьте статус контейнера:
   bashdocker compose ps
2. Посмотрите логи бота:
   bashdocker compose logs telegram-bot
3. Для мониторинга логов в реальном времени:
   bashdocker compose logs -f telegram-bot
4. Проверьте, что бот отвечает в Telegram
   Полезные команды для управления:
   bash# Остановить
   docker compose down

# Перезапустить
docker compose restart telegram-bot

# Посмотреть использование ресурсов
docker stats

# Обновить проект из Git
git pull && docker compose up -d --build