package com.example;

import com.teamHelper.bot.BotComponent;
import com.teamHelper.calendar.YandexCalDavService;
import com.teamHelper.calendar.YandexCalendarService;
import com.teamHelper.model.CalendarEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для YandexCalendarService")
class YandexCalendarServiceTest {

    @Mock
    private YandexCalDavService calDavService;

    @Mock
    private BotComponent bot;

    private YandexCalendarService calendarService;

    @BeforeEach
    void setUp() {
        calendarService = new YandexCalendarService(calDavService, bot);
    }

    @Test
    @DisplayName("Должен проверять предстоящие события без ошибок")
    void shouldCheckUpcomingEventsWithoutErrors() throws Exception {
        when(calDavService.getUpcomingEvents()).thenReturn(Collections.emptyList());

        calendarService.checkUpcomingEvents();

        verify(calDavService).getUpcomingEvents();
        verify(bot, never()).sendErrorMessage(anyString());
    }

    @Test
    @DisplayName("Должен отправлять уведомление о событии, которое скоро начнется")
    void shouldSendNotificationForUpcomingEvent() throws Exception {
        CalendarEvent event = createEventInFuture(3); // Событие через 3 минуты
        when(calDavService.getUpcomingEvents()).thenReturn(Arrays.asList(event));

        calendarService.checkUpcomingEvents();

        verify(bot).sendCalendarNotification(event);
    }

    @Test
    @DisplayName("Должен не отправлять уведомление о событии, которое начнется не скоро")
    void shouldNotSendNotificationForDistantEvent() throws Exception {
        CalendarEvent event = createEventInFuture(10); // Событие через 10 минут
        when(calDavService.getUpcomingEvents()).thenReturn(Arrays.asList(event));

        calendarService.checkUpcomingEvents();

        verify(bot, never()).sendCalendarNotification(any());
    }

    @Test
    @DisplayName("Должен не отправлять повторное уведомление о том же событии")
    void shouldNotSendDuplicateNotification() throws Exception {
        CalendarEvent event = createEventInFuture(3);
        when(calDavService.getUpcomingEvents()).thenReturn(Arrays.asList(event));

        // Первый вызов - должно отправить уведомление
        calendarService.checkUpcomingEvents();
        verify(bot).sendCalendarNotification(event);

        // Второй вызов - не должно отправить повторное уведомление
        calendarService.checkUpcomingEvents();
        verify(bot, times(1)).sendCalendarNotification(event);
    }

    @Test
    @DisplayName("Должен отправлять сообщение об ошибке при неудачной проверке событий")
    void shouldSendErrorMessageOnFailedEventCheck() throws Exception {
        when(calDavService.getUpcomingEvents()).thenThrow(new RuntimeException("Connection failed"));

        calendarService.checkUpcomingEvents();

        verify(bot).sendErrorMessage(contains("Connection failed"));
    }

    @Test
    @DisplayName("Должен очищать кэш уведомленных событий")
    void shouldClearNotifiedEventsCache() throws Exception {
        CalendarEvent event = createEventInFuture(3);
        when(calDavService.getUpcomingEvents()).thenReturn(Arrays.asList(event));

        // Отправляем уведомление
        calendarService.checkUpcomingEvents();
        verify(bot).sendCalendarNotification(event);

        // Очищаем кэш
        calendarService.clearNotifiedEventsCache();

        // Проверяем снова - должно отправить уведомление повторно
        calendarService.checkUpcomingEvents();
        verify(bot, times(2)).sendCalendarNotification(event);
    }

    @Test
    @DisplayName("Должен тестировать соединение CalDAV при инициализации")
    void shouldTestCalDavConnectionOnInit() throws Exception {
        calendarService.init();

        verify(calDavService).testCalDavConnection();
    }

    @Test
    @DisplayName("Должен отправлять ошибку при неудачном тесте соединения")
    void shouldSendErrorOnFailedConnectionTest() throws Exception {
        doThrow(new RuntimeException("Connection failed")).when(calDavService).testCalDavConnection();

        calendarService.init();

        verify(bot).sendErrorMessage(contains("Connection failed"));
    }

    private CalendarEvent createEventInFuture(int minutesFromNow) {
        CalendarEvent event = new CalendarEvent();
        event.setId("test-event-" + minutesFromNow);
        event.setTitle("Test Event");
        event.setStart(LocalDateTime.now().plusMinutes(minutesFromNow));
        event.setEnd(LocalDateTime.now().plusMinutes(minutesFromNow + 60));
        return event;
    }
}
