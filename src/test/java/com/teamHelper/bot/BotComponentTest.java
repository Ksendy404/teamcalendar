package com.teamHelper.bot;

import com.teamHelper.model.CalendarEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

class BotComponentTest {

    private BotComponent bot;

    @BeforeEach
    void setup() {
        bot = spy(new BotComponent("mock-token"));
    }

    @Test
    void shouldSendCalendarNotification() throws Exception {
        CalendarEvent event = new CalendarEvent();
        event.setId("event1");
        event.setTitle("Test Event");
        event.setStart(LocalDateTime.now().plusMinutes(5));

        doReturn(null).when(bot).execute(any(SendMessage.class));

        bot.sendCalendarNotification(event, 123L);

        verify(bot, times(1)).execute(any(SendMessage.class));
    }

    @Test
    void shouldSendErrorMessageWithReflection() throws Exception {
        Field field = BotComponent.class.getDeclaredField("errorChatId");
        field.setAccessible(true);
        field.set(bot, "123456"); // вручную задаём chatId через reflection

        doReturn(null).when(bot).execute(any(SendMessage.class));
        bot.sendErrorMessage("Ошибка соединения");

        verify(bot, times(1)).execute(any(SendMessage.class));
    }
}
