package com.teamHelper.integration;

import com.teamHelper.bot.BotComponent;
import com.teamHelper.calendar.MultiCalendarService;
import com.teamHelper.calendar.YandexCalendarService;
import com.teamHelper.model.CalendarEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

class YandexCalendarServiceSimpleTest {

//    @Test
//    void shouldCallBotForUpcomingEvent() {
//        // given
//        MultiCalendarService multiService = mock(MultiCalendarService.class);
//        BotComponent bot = mock(BotComponent.class);
//        YandexCalendarService service = new YandexCalendarService(multiService, bot);
//
//        CalendarEvent event = new CalendarEvent();
//        event.setId("123");
//        event.setTitle("Test");
//        event.setStart(LocalDateTime.now().plusMinutes(5));
//
//        when(multiService.getAllEventsForToday())
//                .thenReturn(List.of(new MultiCalendarService.EventWithChat(event, 999L)));
//
//        // when
//        service.refreshCalendarCache();
//        service.checkEvents();
//
//        // then
//        verify(bot).sendCalendarNotification(eq(event), eq(999L));
//    }
}