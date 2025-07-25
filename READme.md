Telegram-бот для уведомлений о событиях календаря, который интегрируется с Yandex CalDAV.

Для использования добавьте файл .env с пропертями

```
# Глобальные параметры бота
TELEGRAM_BOT_TOKEN=
TELEGRAM_BOT_USERNAME=
TELEGRAM_BOT_ADMIN_CHAT_ID=
TELEGRAM_BOT_ERROR_CHAT_ID=

# Доступ к Яндекс.Календарю (общий логин/пароль)
CALDAV_USERNAME= 
CALDAV_PASS=мой_пароль

# Количество календарей
CALENDAR_COUNT=2

# Первый календарь (уникальный только URL + чат)
CALENDAR_1_ID=team-events
CALENDAR_1_URL=https://caldav.yandex.ru/calendars/itTeamHelper@yandex.com/events-11111/
CALENDAR_1_CHAT_ID=-222222222

# Второй календарь
CALENDAR_2_ID=project-deadlines
CALENDAR_2_URL=https://caldav.yandex.ru/calendars/itTeamHelper@yandex.com/events-22222/
CALENDAR_2_CHAT_ID=-111111111
```

Для запуска локально используйте 
```
set -a && source .env && set +a && mvn spring-boot:run
```

Для запуска в докер
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
```
# Перезапустить
docker compose restart telegram-bot
```
```
# Посмотреть использование ресурсов
docker stats
```
```
# Обновить проект из Git
git pull && docker compose up -d --build