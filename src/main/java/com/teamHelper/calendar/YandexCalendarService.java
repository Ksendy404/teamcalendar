
package com.teamHelper.calendar;

import com.teamHelper.bot.BotComponent;
import com.teamHelper.calendar.MultiCalendarService.EventWithChat;
import com.teamHelper.config.CalendarAccountConfig;
import com.teamHelper.config.CalendarAccountsProperties;
import com.teamHelper.model.CalendarEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class YandexCalendarService {

    private final MultiCalendarService multiCalendarService;
    private final BotComponent bot;

    private final int NOTIFY_BEFORE_MINUTES = 5;
    private final int POLL_WINDOW_MINUTES = 10;
    private final LocalTime WORK_START = LocalTime.of(8, 55);
    private final LocalTime WORK_END = LocalTime.of(18, 0);
    private final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private volatile List<EventWithChat> cachedEvents = new ArrayList<>();
    private final Set<String> notifiedEventIds = new HashSet<>();
    private final AtomicBoolean isFirstCheckToday = new AtomicBoolean(true);
    private final CalendarAccountsProperties calendarAccounts;
    private final YandexCalDavService calDavService;


    @PostConstruct
    public void init() {
        log.info("🚀 Старт YandexCalendarService");

        List<CalendarEvent> allEvents = new ArrayList<>();

        for (CalendarAccountConfig account : calendarAccounts.getAccounts()) {
            try {
                log.debug("🔗 Подключаюсь к календарю {}", account.getId());

                List<CalendarEvent> events = calDavService.getUpcomingEvents(account);

                allEvents.addAll(events);
            } catch (Exception e) {
                log.error("❌ Ошибка при получении событий из календаря {}: {}", account.getId(), e.getMessage(), e);
            }
        }

        log.info("♻️ Календарь обновлён: {} событий", allEvents.size());
    }

    @Scheduled(cron = "0 */5 8-18 * * MON-FRI") // обновляем кэш каждые 5 мин
    public void refreshCalendarCache() {
        try {
            List<EventWithChat> events = multiCalendarService.getAllEventsForToday();
            cachedEvents = events;
            log.info("♻️ Календарь обновлён: {} событий", events.size());
        } catch (Exception e) {
            log.error("Ошибка обновления календаря: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 * 8-18 * * MON-FRI") // проверка каждую минуту
    public void checkEvents() {
        if (isFirstCheckToday.getAndSet(false)) {
            logDailyEventsSummary(cachedEvents);
        }

        LocalDateTime now = LocalDateTime.now();

        List<EventWithChat> relevant = cachedEvents.stream()
                .filter(e -> isWithinPollWindow(e.event(), now))
                .collect(Collectors.toList());

        long sent = relevant.stream()
                .filter(e -> shouldNotify(e.event(), now))
                .peek(e -> bot.sendCalendarNotification(e.event(), e.chatId()))
                .count();

        if (sent > 0) {
            log.info("📨 Отправлено {} уведомлений", sent);
        }
    }

    private boolean isWithinPollWindow(CalendarEvent event, LocalDateTime now) {
        if (event.getStart() == null) return false;
        Duration until = Duration.between(now, event.getStart());
        return !until.isNegative() && until.toMinutes() <= POLL_WINDOW_MINUTES;
    }

    private boolean shouldNotify(CalendarEvent event, LocalDateTime now) {
        if (event.getStart() == null || notifiedEventIds.contains(event.getId())) return false;

        Duration until = Duration.between(now, event.getStart());
        long seconds = until.getSeconds();

        boolean inWindow = !until.isNegative()
                && seconds <= (NOTIFY_BEFORE_MINUTES * 60)
                && seconds > ((NOTIFY_BEFORE_MINUTES - 1) * 60);

        if (inWindow) {
            notifiedEventIds.add(event.getId());
        }

        return inWindow;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void clearNotifiedCache() {
        int cleared = notifiedEventIds.size();
        notifiedEventIds.clear();
        isFirstCheckToday.set(true);
        log.info("🧹 Очищен кэш уведомлений: {} записей", cleared);
    }

    private void logDailyEventsSummary(List<EventWithChat> events) {
        log.info("📅 События на сегодня ({}):", events.size());
        events.stream()
                .map(EventWithChat::event)
                .filter(e -> e.getStart() != null)
                .sorted(Comparator.comparing(CalendarEvent::getStart))
                .forEach(e -> log.info("  • {} — {}", e.getStart().format(TIME_FORMATTER), e.getTitle()));
    }

    private void checkMissedEvents() {
        LocalDateTime now = LocalDateTime.now();

        long count = cachedEvents.stream()
                .filter(e -> {
                    LocalDateTime start = e.event().getStart();
                    return start != null &&
                            !start.isBefore(now.minusMinutes(POLL_WINDOW_MINUTES)) &&
                            !start.isAfter(now.plusMinutes(NOTIFY_BEFORE_MINUTES));
                })
                .peek(e -> {
                    bot.sendCalendarNotification(e.event(), e.chatId());
                    notifiedEventIds.add(e.event().getId());
                })
                .count();

        if (count > 0) {
            log.info("🔁 Пропущено и доставлено {} событий", count);
        }
    }
}
