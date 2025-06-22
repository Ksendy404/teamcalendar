package com.teamHelper.calendar;

import com.teamHelper.bot.BotComponent;
import com.teamHelper.calendar.MultiCalendarService.EventWithChat;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class YandexCalendarService {

    private final MultiCalendarService multiCalendarService;
    private final BotComponent bot;

    private final int CHECK_INTERVAL_MINUTES = 1;
    private final int NOTIFY_BEFORE_MINUTES = 5;
    private final LocalTime WORK_START = LocalTime.of(9, 0);
    private final LocalTime WORK_END = LocalTime.of(18, 0);

    private final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final Set<String> notifiedEvents = new HashSet<>();
    private boolean isFirstCheckToday = true;

    @PostConstruct
    public void init() {
        log.info("🚀 Запуск многокалендарного сервиса уведомлений");

        LocalTime now = LocalTime.now();
        if (!now.isBefore(WORK_START) && !now.isAfter(WORK_END)) {
            checkMissedEvents();
        }
    }

    @Scheduled(fixedRate = CHECK_INTERVAL_MINUTES * 60 * 1000)
    public void checkEvents() {
        try {
            List<EventWithChat> eventsWithChat = multiCalendarService.getAllEventsForToday();

            if (isFirstCheckToday) {
                logDailyEventsSummary(eventsWithChat);
                isFirstCheckToday = false;
            }

            long notifiedCount = eventsWithChat.stream()
                    .peek(e -> log.debug("🔍 Проверяю: '{}' на {} для чата {}",
                            e.event().getTitle(), e.event().getStart(), e.chatId()))
                    .mapToLong(e -> processEvent(e.event(), e.chatId()) ? 1 : 0)
                    .sum();

            if (notifiedCount > 0) {
                log.info("📨 Отправлено {} уведомлений", notifiedCount);
            }

        } catch (Exception e) {
            log.error("❌ Ошибка при проверке событий: {}", e.getMessage());
            bot.sendErrorMessage("Ошибка получения событий: " + e.getMessage());
        }
    }

    private boolean processEvent(CalendarEvent event, Long chatId) {
        if (shouldNotify(event)) {
            log.info("Отправляю: '{}' ({}), чат {}",
                    event.getTitle(), event.getStart().format(TIME_FORMATTER), chatId);

            try {
                bot.sendCalendarNotification(event, chatId);
                return true;
            } catch (Exception e) {
                log.error("❌ Ошибка отправки уведомления '{}': {}", event.getTitle(), e.getMessage());
                return false;
            }
        }
        return false;
    }

    private boolean shouldNotify(CalendarEvent event) {
        if (event.getStart() == null || notifiedEvents.contains(event.getId())) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        Duration timeUntilEvent = Duration.between(now, event.getStart());

        long seconds = timeUntilEvent.getSeconds();
        boolean shouldNotify = !timeUntilEvent.isNegative()
                && seconds <= (NOTIFY_BEFORE_MINUTES * 60)
                && seconds > ((NOTIFY_BEFORE_MINUTES - 1) * 60);

        if (shouldNotify) {
            notifiedEvents.add(event.getId());
        }

        return shouldNotify;
    }

    private void logDailyEventsSummary(List<EventWithChat> events) {
        log.info("📅 События на сегодня ({}):", events.size());
        events.stream()
                .map(EventWithChat::event)
                .filter(e -> e.getStart() != null)
                .sorted((e1, e2) -> e1.getStart().compareTo(e2.getStart()))
                .forEach(e -> log.info("  • {} — {}", e.getStart().format(TIME_FORMATTER), e.getTitle()));
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void clearNotifiedEventsCache() {
        int cleared = notifiedEvents.size();
        notifiedEvents.clear();
        isFirstCheckToday = true;
        if (cleared > 0) {
            log.info("🧹 Кеш уведомлений очищен: {} записей", cleared);
        }
    }

    private void checkMissedEvents() {
        try {
            List<EventWithChat> eventsWithChat = multiCalendarService.getAllEventsForToday();

            long count = eventsWithChat.stream()
                    .filter(e -> e.event().getStart().isAfter(LocalDateTime.now().minusMinutes(5)))
                    .filter(e -> e.event().getStart().isBefore(LocalDateTime.now().plusMinutes(NOTIFY_BEFORE_MINUTES)))
                    .peek(e -> {
                        log.info("⏰ Пропущенное событие: '{}' ({})",
                                e.event().getTitle(), e.event().getStart().format(TIME_FORMATTER));
                        processEvent(e.event(), e.chatId());
                    })
                    .count();

            if (count > 0) {
                log.info("🔁 Обработано {} пропущенных событий", count);
            }

        } catch (Exception e) {
            log.error("Ошибка при проверке пропущенных событий: {}", e.getMessage());
            bot.sendErrorMessage("Ошибка при проверке пропущенных событий: " + e.getMessage());
        }
    }
}