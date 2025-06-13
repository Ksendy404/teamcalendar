package com.teamHelper.calendar;

import com.teamHelper.bot.BotComponent;
import com.teamHelper.model.CalendarEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.teamHelper.calendar.CalendarConstants.CHECK_INTERVAL_MINUTES;
import static com.teamHelper.calendar.CalendarConstants.NOTIFY_BEFORE_MINUTES;

@Slf4j
@Service
@RequiredArgsConstructor
public class YandexCalendarService {

    private final YandexCalDavService calDavService;
    private final BotComponent bot;
    private final Set<String> notifiedEvents = new HashSet<>();

    // Рабочие часы
    private static final LocalTime WORK_START = LocalTime.of(9, 0);
    private static final LocalTime WORK_END = LocalTime.of(18, 0);

    @Scheduled(fixedRate = CHECK_INTERVAL_MINUTES * 60 * 1000)
    public void checkUpcomingEvents() {
        // Проверяем, рабочее ли время
        LocalTime now = LocalTime.now();
        if (now.isBefore(WORK_START) || now.isAfter(WORK_END)) {
            log.debug("Вне рабочих часов ({} - {}), проверка календаря пропущена", WORK_START, WORK_END);
            return;
        }

        try {
            List<CalendarEvent> events = calDavService.getUpcomingEvents();

            if (events.isEmpty()) {
                return;
            }

            long notifiedCount = events.stream()
                    .peek(event -> log.debug("🔍 Проверяю событие: '{}' на {}", event.getTitle(), event.getStart()))
                    .mapToLong(event -> processEvent(event) ? 1 : 0)
                    .sum();

            if (notifiedCount > 0) {
                log.info("Отправлено {} уведомлений о событиях", notifiedCount);
            }

        } catch (Exception e) {
            log.error("Ошибка проверки событий: {}", e.getMessage());
            bot.sendErrorMessage("Ошибка получения событий: " + e.getMessage());
        }
    }

    private boolean processEvent(CalendarEvent event) {
        if (shouldNotify(event)) {
            log.info("Готовлюсь отправить уведомление о событии: '{}' запланированном на {}",
                    event.getTitle(), event.getStart());

            try {
                bot.sendCalendarNotification(event);
                log.info("✅ Уведомление о событии '{}' успешно отправлено", event.getTitle());
                return true;
            } catch (Exception e) {
                log.error("❌ Ошибка отправки уведомления о событии '{}': {}", event.getTitle(), e.getMessage());
                return false;
            }
        }
        return false;
    }

    @PostConstruct
    public void init() {
        log.info("Запуск календарного сервиса (проверка: {} мин, уведомление: {} мин до события, рабочие часы: {} - {})",
                CHECK_INTERVAL_MINUTES, NOTIFY_BEFORE_MINUTES, WORK_START, WORK_END);

        try {
            calDavService.testCalDavConnection();
            log.info("CalDAV подключение успешно");
        } catch (Exception e) {
            log.error("Ошибка подключения CalDAV: {}", e.getMessage());
            bot.sendErrorMessage("Ошибка подключения к CalDAV: " + e.getMessage());
        }

        // Проверяем пропущенные события только в рабочее время
        LocalTime now = LocalTime.now();
        if (!now.isBefore(WORK_START) && !now.isAfter(WORK_END)) {
            checkMissedEvents();
        }
    }

    private void checkMissedEvents() {
        try {
            List<CalendarEvent> events = calDavService.getUpcomingEvents();

            long missedCount = events.stream()
                    .filter(e -> e.getStart().isAfter(LocalDateTime.now().minusMinutes(5)))
                    .filter(e -> e.getStart().isBefore(LocalDateTime.now().plusMinutes(NOTIFY_BEFORE_MINUTES)))
                    .peek(e -> {
                        log.info("🔍 Найдено пропущенное событие: '{}' запланированное на {}", e.getTitle(), e.getStart());
                        processEvent(e);
                    })
                    .count();

            if (missedCount > 0) {
                log.info("📋 Обработано {} пропущенных событий", missedCount);
            }

        } catch (Exception e) {
            log.error("Ошибка проверки пропущенных событий: {}", e.getMessage());
            bot.sendErrorMessage("Ошибка проверки пропущенных событий: " + e.getMessage());
        }
    }

    private boolean shouldNotify(CalendarEvent event) {
        if (event.getStart() == null || notifiedEvents.contains(event.getId())) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        Duration timeUntilEvent = Duration.between(now, event.getStart());
        long minutes = timeUntilEvent.toMinutes();

        boolean shouldNotify = !timeUntilEvent.isNegative()
                && minutes <= NOTIFY_BEFORE_MINUTES;

        if (shouldNotify) {
            notifiedEvents.add(event.getId());
        }

        return shouldNotify;
    }

    @Scheduled(cron = "0 0 0 * * ?") // Каждый день в полночь очистка кеша
    public void clearNotifiedEventsCache() {
        int clearedCount = notifiedEvents.size();
        notifiedEvents.clear();
        if (clearedCount > 0) {
            log.info("Очищен кеш уведомленных событий: {} записей", clearedCount);
        }
    }
}