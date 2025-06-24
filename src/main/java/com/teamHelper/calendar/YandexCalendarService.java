
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private final Set<String> sentEvents = new HashSet<>();

    @Scheduled(cron = "0 * * * * *") // Каждую минуту
    public void updateCalendar() {
        if (LocalTime.now().isAfter(WORK_END)) {
          //  log.info("⏳ После " + WORK_END + " — не проверяем события");
            return;
        }

        log.info(" Старт YandexCalendarService");
        List<CalendarEvent> allEvents = new ArrayList<>();

        for (var account : calendarAccounts.getAccounts()) {
            try {
                log.debug("Подключаюсь к календарю {}", account.getId());
                List<CalendarEvent> events = calDavService.getUpcomingEvents(account);

                List<CalendarEvent> todayEvents = events.stream()
                        .filter(e -> e.getStart().toLocalDate().equals(LocalDate.now()))
                        .toList();

                todayEvents.forEach(event -> {
                    if (shouldSendNotification(event)) {
                        bot.sendCalendarNotification(event, account.getTelegramChatId());
                        sentEvents.add(event.getId());
                    }
                });

                log.info("Получено {} событий из календаря {}", todayEvents.size(), account.getId());
                allEvents.addAll(todayEvents);
            } catch (Exception e) {
                log.error("Ошибка при получении событий из календаря {}: {}", account.getId(), e.getMessage(), e);
            }
        }

        cachedEvents.clear();
        cachedEvents.addAll(allEvents);

        log.info("♻️ Календарь обновлён: {} событий", allEvents.size());
    }

    private boolean shouldSendNotification(CalendarEvent event) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime inFiveMinutes = now.plusMinutes(5);

        return !sentEvents.contains(event.getId())
                && event.getStart().isAfter(now)
                && event.getStart().isBefore(inFiveMinutes);
    }

    @PostConstruct
    public void init() {
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
                    sentEvents.add(event.getId());
                });

            } catch (Exception e) {
                log.error("Ошибка при проверке событий календаря {}: {}", account.getId(), e.getMessage());
            }
        });
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void clearCache() {
        log.info("🧹 Очистка кэша календаря и сброс отправленных событий");
        cachedEvents.clear();
        sentEvents.clear();
    }
}