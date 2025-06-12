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

    @Scheduled(fixedRate = CHECK_INTERVAL_MINUTES * 60 * 1000)
    public void checkUpcomingEvents() {
        try {
            log.debug("Checking events for next {} minutes", CHECK_INTERVAL_MINUTES);
            List<CalendarEvent> events = calDavService.getUpcomingEvents();

            if (events.isEmpty()) {
                log.debug("No events found");
                return;
            }

            log.debug("Processing {} events", events.size());
            long notifiedCount = events.stream()
                    .peek(event -> log.trace("Processing event: {}", event.getTitle()))
                    .mapToLong(event -> processEvent(event) ? 1 : 0)
                    .sum();

            if (notifiedCount > 0) {
                log.info("Sent {} event notifications", notifiedCount);
            }

        } catch (Exception e) {
            log.error("Event check failed: {}", e.getMessage());
            log.debug("Event check error details", e);
            bot.sendErrorMessage("Ошибка получения событий: " + e.getMessage());
        }
    }

    private boolean processEvent(CalendarEvent event) {
        if (shouldNotify(event)) {
            log.info("Sending notification for event: {}", event.getTitle());
            bot.sendCalendarNotification(event);
            return true;
        }
        return false;
    }

    @PostConstruct
    public void init() {
        log.info("Starting calendar service (check: {}min, notify: {}min before)",
                CHECK_INTERVAL_MINUTES, NOTIFY_BEFORE_MINUTES);

        try {
            calDavService.testCalDavConnection();
            log.info("CalDAV connection successful");
        } catch (Exception e) {
            log.error("CalDAV connection failed: {}", e.getMessage());
            log.debug("CalDAV connection error details", e);
            bot.sendErrorMessage("Ошибка подключения к CalDAV: " + e.getMessage());
        }

        checkMissedEvents();
    }

    private void checkMissedEvents() {
        try {
            log.debug("Checking for missed events");
            List<CalendarEvent> events = calDavService.getUpcomingEvents();

            long missedCount = events.stream()
                    .peek(e -> log.trace("Checking missed event: {} at {}", e.getTitle(), e.getStart()))
                    .filter(e -> e.getStart().isAfter(LocalDateTime.now().minusMinutes(5)))
                    .filter(e -> e.getStart().isBefore(LocalDateTime.now().plusMinutes(NOTIFY_BEFORE_MINUTES)))
                    .peek(e -> {
                        log.info("Found missed event: {} at {}", e.getTitle(), e.getStart());
                        processEvent(e);
                    })
                    .count();

            if (missedCount > 0) {
                log.info("Processed {} missed events", missedCount);
            } else {
                log.debug("No missed events found");
            }

        } catch (Exception e) {
            log.error("Missed events check failed: {}", e.getMessage());
            log.debug("Missed events check error details", e);
            bot.sendErrorMessage("Ошибка проверки пропущенных событий: " + e.getMessage());
        }
    }

    private boolean shouldNotify(CalendarEvent event) {
        if (event.getStart() == null || notifiedEvents.contains(event.getId())) {
            log.trace("Skipping event '{}': {} (start null: {}, already notified: {})",
                    event.getTitle(), event.getId(),
                    event.getStart() == null,
                    notifiedEvents.contains(event.getId()));
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        Duration timeUntilEvent = Duration.between(now, event.getStart());
        long minutes = timeUntilEvent.toMinutes();

        boolean shouldNotify = !timeUntilEvent.isNegative()
                && minutes <= NOTIFY_BEFORE_MINUTES;

        log.trace("Event '{}': {} minutes until start (should notify: {})",
                event.getTitle(), minutes, shouldNotify);

        if (shouldNotify) {
            notifiedEvents.add(event.getId());
            log.debug("Event '{}' scheduled for notification ({} minutes until start)",
                    event.getTitle(), minutes);
        }

        return shouldNotify;
    }

    @Scheduled(cron = "0 0 0 * * ?") // Каждый день в полночь очистка кеша
    public void clearNotifiedEventsCache() {
        int clearedCount = notifiedEvents.size();
        notifiedEvents.clear();
        log.info("Cleared {} notified events from cache", clearedCount);
    }
}