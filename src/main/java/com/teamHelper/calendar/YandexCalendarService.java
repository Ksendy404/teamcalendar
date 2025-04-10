package com.teamHelper.calendar;

import com.teamHelper.bot.BotComponent;
import com.teamHelper.pojo.CalendarEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class YandexCalendarService {

    private final YandexCalDavService calDavService;
    private final BotComponent bot;
    private final Set<String> notifiedEvents = new HashSet<>();


    // Настройки уведомлений
    private static final int CHECK_INTERVAL_MINUTES = 1;  // Интервал проверки (в минутах)
    private static final int NOTIFY_BEFORE_MINUTES = 5;   // Уведомлять за N минут до события

    @Scheduled(fixedRate = CHECK_INTERVAL_MINUTES * 60 * 1000)
    public void checkUpcomingEvents() {
        try {
            log.info("Checking events for next {} minutes", CHECK_INTERVAL_MINUTES);
            List<CalendarEvent> events = calDavService.getUpcomingEvents();
            log.info("Found {} events", events.size());
            events.forEach(event -> {
                log.debug("Processing event: {}", event.getTitle());
                processEvent(event);
            });
        } catch (Exception e) {
            log.error("Event check failed", e);
            bot.sendErrorMessage("Ошибка получения событий: " + e.getMessage());
        }
    }

    private void processEvent(CalendarEvent event) {
        if (shouldNotify(event)) {
            bot.sendCalendarNotification(event);
        }
    }

    @PostConstruct
    public void init() {
        log.info("Starting calendar service with settings:");
        log.info("Check interval: {} minutes", CHECK_INTERVAL_MINUTES);
        log.info("Notify before: {} minutes", NOTIFY_BEFORE_MINUTES);

        try {
            calDavService.testCalDavConnection();
            log.info("CalDAV connection test successful");
        } catch (Exception e) {
            log.error("CalDAV connection test failed", e);
            bot.sendErrorMessage("Ошибка подключения к CalDAV: " + e.getMessage());
        }

        checkMissedEvents();
    }

    private void checkMissedEvents() {
        try {
            log.info("Checking for missed events");
            List<CalendarEvent> events = calDavService.getUpcomingEvents();

            long found = events.stream()
                    .peek(e -> log.debug("Candidate event: {} at {}", e.getTitle(), e.getStart()))
                    .filter(e -> e.getStart().isAfter(LocalDateTime.now().minusMinutes(5)))
                    .filter(e -> e.getStart().isBefore(LocalDateTime.now().plusMinutes(NOTIFY_BEFORE_MINUTES)))
                    .peek(e -> log.info("Found missed event: {}", e.getTitle()))
                    .count();

            log.info("Missed events check completed. Found: {}", found);
        } catch (Exception e) {
            log.error("Missed events check failed", e);
            bot.sendErrorMessage("Ошибка проверки пропущенных событий: " + e.getMessage());
        }
    }
    private boolean shouldNotify(CalendarEvent event) {
        if (event.getStart() == null || notifiedEvents.contains(event.getId())) {
            return false;
        }

        Duration timeUntilEvent = Duration.between(LocalDateTime.now(), event.getStart());
        long minutes = timeUntilEvent.toMinutes();

        boolean shouldNotify = !timeUntilEvent.isNegative()
                && timeUntilEvent.toMinutes() <= NOTIFY_BEFORE_MINUTES;

        if (shouldNotify) {
            notifiedEvents.add(event.getId());
        }

        log.debug("Checking event '{}': {} minutes left (now: {}, event: {})",
                event.getTitle(),
                minutes,
                LocalDateTime.now(),
                event.getStart());

        return shouldNotify;
    }

    @Scheduled(cron = "0 0 0 * * ?") // Каждый день в полночь
    public void clearNotifiedEventsCache() {
        notifiedEvents.clear();
        log.info("Cleared notified events cache");
    }

}