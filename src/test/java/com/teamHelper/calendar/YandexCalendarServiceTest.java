package com.teamHelper.calendar;

import com.teamHelper.bot.BotComponent;
import com.teamHelper.calendar.MultiCalendarService.EventWithChat;
import com.teamHelper.model.CalendarEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

class YandexCalendarServiceTest {

    private MultiCalendarService multiCalendarService;
    private BotComponent bot;
    private YandexCalendarService service;

    @BeforeEach
    void setup() {
        multiCalendarService = mock(MultiCalendarService.class);
        bot = mock(BotComponent.class);
        service = new YandexCalendarService(multiCalendarService, bot);
    }

    @Test
    void shouldSendNotificationWithin5Minutes() {
        CalendarEvent event = new CalendarEvent();
        event.setId("evt1");
        event.setTitle("Встреча через 5 минут");
        event.setStart(LocalDateTime.now().plusMinutes(5));

        EventWithChat wrapped = new EventWithChat(event, 123L);

        when(multiCalendarService.getAllEventsForToday()).thenReturn(List.of(wrapped));

        service.refreshCalendarCache();
        service.checkEvents();

        verify(bot, times(1)).sendCalendarNotification(eq(event), eq(123L));
    }

    @Test
    void shouldNotSendTwiceForSameEvent() {
        CalendarEvent event = new CalendarEvent();
        event.setId("evt2");
        event.setTitle("Встреча уже была");
        event.setStart(LocalDateTime.now().plusMinutes(5));

        EventWithChat wrapped = new EventWithChat(event, 123L);

        when(multiCalendarService.getAllEventsForToday()).thenReturn(List.of(wrapped));

        service.refreshCalendarCache();
        service.checkEvents(); // first time
        service.checkEvents(); // second time

        verify(bot, times(1)).sendCalendarNotification(eq(event), eq(123L));
    }

    @Test
    void shouldSkipFarFutureEvent() {
        CalendarEvent event = new CalendarEvent();
        event.setId("future1");
        event.setTitle("Далёкое событие");
        event.setStart(LocalDateTime.now().plusHours(3));

        EventWithChat wrapped = new EventWithChat(event, 123L);

        when(multiCalendarService.getAllEventsForToday()).thenReturn(List.of(wrapped));

        service.refreshCalendarCache();
        service.checkEvents();

        verify(bot, never()).sendCalendarNotification(any(), any());
    }

    @Test
    void shouldSendMissedEvent() {
        CalendarEvent event = new CalendarEvent();
        event.setId("late1");
        event.setTitle("Опоздавшее событие");
        event.setStart(LocalDateTime.now().minusMinutes(5));

        EventWithChat wrapped = new EventWithChat(event, 123L);

        when(multiCalendarService.getAllEventsForToday()).thenReturn(List.of(wrapped));

        service.refreshCalendarCache();
        service.init();

        verify(bot).sendCalendarNotification(eq(event), eq(123L));
    }
}
