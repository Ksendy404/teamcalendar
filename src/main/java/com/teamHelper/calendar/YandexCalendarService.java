
package com.teamHelper.calendar;

import com.teamHelper.bot.BotComponent;
import com.teamHelper.config.CalendarAccountsProperties;
import com.teamHelper.model.CalendarEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.teamHelper.calendar.CalendarConstants.WORK_END;
import static com.teamHelper.calendar.CalendarConstants.WORK_START;

@Slf4j
@Service
@RequiredArgsConstructor
public class YandexCalendarService {

    private final CalendarAccountsProperties calendarAccounts;
    private final YandexCalDavService calDavService;
    private final BotComponent bot;
    private final List<CalendarEvent> cachedEvents = new CopyOnWriteArrayList<>();

    @Scheduled(cron = "0 */2 * * * *") // каждые 2 минуты
    public void updateCalendar() {
        if (LocalTime.now().isAfter(WORK_END)) {
          //  log.info("⏳ После " + WORK_END + " — не проверяем события");
            return;
        }

        log.info("Старт YandexCalendarService");

        List<CalendarEvent> allEvents = new ArrayList<>();

        for (var account : calendarAccounts.getAccounts()) {
            try {
                log.debug("Подключаюсь к календарю {}", account.getId());

                List<CalendarEvent> events = calDavService.getUpcomingEvents(account);
                events = events.stream()
                        .filter(e -> e.getStart().toLocalDate().equals(LocalDate.now()))
                        .toList();

                log.info("✅ Получено {} событий из календаря {}", events.size(), account.getId());

                allEvents.addAll(events);
            } catch (Exception e) {
                log.error("❌ Ошибка при получении событий из календаря {}: {}", account.getId(), e.getMessage(), e);
            }
        }

        log.info("♻️ Календарь обновлён: {} событий", allEvents.size());
    }

    @PostConstruct
    public void init() {
        // Проверяем пропущенные события только в рабочее время
        LocalTime now = LocalTime.now();
        if (!now.isBefore(WORK_START) && !now.isAfter(WORK_END)) {
            checkMissedEventsOnStart();
        }
    }

    public void checkMissedEventsOnStart() {
        log.info("🔎 Проверка пропущенных событий при старте бота");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime inFiveMinutes = now.plusMinutes(5);

        calendarAccounts.getAccounts().forEach(account -> {
            log.debug("Проверка календаря {} → {}", account.getId(), account.getUrl());

            try {
                List<CalendarEvent> events = calDavService.getUpcomingEvents(account);
                List<CalendarEvent> missed = events.stream()
                        .filter(event -> {
                            LocalDateTime start = event.getStart();
                            return start.isAfter(now) && start.isBefore(inFiveMinutes);
                        })
                        .toList();

                log.info("Пропущено {} событий из календаря {}", missed.size(), account.getId());

                missed.forEach(event -> {
                    bot.sendCalendarNotification(event, account.getTelegramChatId());
                });

            } catch (Exception e) {
                log.error("Ошибка при проверке событий календаря {}: {}", account.getId(), e.getMessage());
            }
        });
    }

    @Scheduled(cron = "0 0 0 * * *")  // Каждый день в полночь очистка кеша
    public void clearCache() {
        log.info("🧹 Очистка кэша календаря");
        cachedEvents.clear();
    }
}
