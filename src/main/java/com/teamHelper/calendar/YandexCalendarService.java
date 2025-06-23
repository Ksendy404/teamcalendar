
package com.teamHelper.calendar;

import com.teamHelper.bot.BotComponent;
import com.teamHelper.config.CalendarAccountsProperties;
import com.teamHelper.model.CalendarEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class YandexCalendarService {

    private final CalendarAccountsProperties calendarAccounts;
    private final YandexCalDavService calDavService;
    private final BotComponent bot;

    @Scheduled(cron = "0 */5 * * * *") // каждые 5 минут
    public void updateCalendar() {
        if (LocalTime.now().isAfter(LocalTime.of(20, 0))) {
            log.info("⏳ После 20:00 — не проверяем события");
            return;
        }

        log.info("🚀 Старт YandexCalendarService");

        List<CalendarEvent> allEvents = new ArrayList<>();

        for (var account : calendarAccounts.getAccounts()) {
            try {
                log.debug("🔗 Подключаюсь к календарю {}", account.getId());

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
}
