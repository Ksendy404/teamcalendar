package com.example;

import com.teamHelper.bot.MessageBuilder;
import com.teamHelper.model.CalendarEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class MessageBuilderTest {

    private MessageBuilder messageBuilder;

    @BeforeEach
    void setUp() {
        messageBuilder = new MessageBuilder();
    }

    @Test
    @DisplayName("Должен корректно форматировать простое событие")
    void shouldFormatBasicEvent() {
        // Arrange
        CalendarEvent event = new CalendarEvent();
        event.setTitle("Встреча команды");
        event.setStart(LocalDateTime.of(2024, 1, 15, 10, 0));
        event.setEnd(LocalDateTime.of(2024, 1, 15, 11, 0));

        // Act
        String result = messageBuilder.buildEventMessage(event);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("🔔  Встреча команды"));
        assertTrue(result.contains("⏰  `10:00` \\- `11:00`"));
    }

    @Test
    @DisplayName("Должен корректно форматировать событие с описанием")
    void shouldFormatEventWithDescription() {
        // Arrange
        CalendarEvent event = new CalendarEvent();
        event.setTitle("Планирование спринта");
        event.setDescription("Обсуждение задач на следующий спринт");
        event.setStart(LocalDateTime.of(2024, 1, 15, 14, 0));
        event.setEnd(LocalDateTime.of(2024, 1, 15, 15, 30));

        // Act
        String result = messageBuilder.buildEventMessage(event);

        // Assert
        assertTrue(result.contains("Планирование спринта"));
        assertTrue(result.contains("Обсуждение задач на следующий спринт"));
        assertTrue(result.contains("`14:00` \\- `15:30`"));
    }

    @Test
    @DisplayName("Должен корректно форматировать событие с локацией")
    void shouldFormatEventWithLocation() {
        // Arrange
        CalendarEvent event = new CalendarEvent();
        event.setTitle("Презентация");
        event.setStart(LocalDateTime.of(2024, 1, 15, 16, 0));
        event.setEnd(LocalDateTime.of(2024, 1, 15, 17, 0));

        CalendarEvent.Location location = new CalendarEvent.Location();
        location.setTitle("Конференц-зал А");
        event.setLocation(location);

        // Act
        String result = messageBuilder.buildEventMessage(event);

        // Assert
        assertTrue(result.contains("Презентация"));
        assertTrue(result.contains("*Место\\:* Конференц\\-зал А"));
    }

    @Test
    @DisplayName("Должен корректно экранировать специальные символы MarkdownV2")
    void shouldEscapeMarkdownV2Characters() {
        // Arrange
        String textWithSpecialChars = "Test_with*special[chars]()~`>#+=|{}.!-:";

        // Act
        String result = messageBuilder.escapeMarkdownV2(textWithSpecialChars);

        // Assert
        assertEquals("Test\\_with\\*special\\[chars\\]\\(\\)\\~\\`\\>\\#\\+\\=\\|\\{\\}\\.\\!\\-\\:", result);
    }

    @Test
    @DisplayName("Должен обрабатывать null значения")
    void shouldHandleNullValues() {
        // Arrange
        CalendarEvent event = new CalendarEvent();
        event.setTitle("Событие без деталей");
        event.setStart(LocalDateTime.of(2024, 1, 15, 10, 0));
        event.setEnd(LocalDateTime.of(2024, 1, 15, 11, 0));
        event.setDescription(null);
        event.setLocation(null);

        // Act
        String result = messageBuilder.buildEventMessage(event);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Событие без деталей"));
        assertFalse(result.contains("null"));
    }

    @Test
    @DisplayName("Должен корректно обрабатывать пустые строки")
    void shouldHandleEmptyStrings() {
        // Act
        String result = messageBuilder.escapeMarkdownV2("");

        // Assert
        assertEquals("", result);
    }

    @Test
    @DisplayName("Должен обрабатывать null при экранировании")
    void shouldHandleNullInEscaping() {
        // Act
        String result = messageBuilder.escapeMarkdownV2(null);

        // Assert
        assertEquals("", result);
    }

    @Test
    @DisplayName("Должен корректно форматировать полное событие")
    void shouldFormatCompleteEvent() {
        // Arrange
        CalendarEvent event = new CalendarEvent();
        event.setTitle("Важная встреча!");
        event.setDescription("Обсуждение планов на Q1-2024");
        event.setStart(LocalDateTime.of(2024, 1, 15, 9, 30));
        event.setEnd(LocalDateTime.of(2024, 1, 15, 10, 45));

        CalendarEvent.Location location = new CalendarEvent.Location();
        location.setTitle("Zoom (ссылка в описании)");
        event.setLocation(location);

        // Act
        String result = messageBuilder.buildEventMessage(event);

        // Assert
        assertTrue(result.contains("🔔  Важная встреча\\!"));
        assertTrue(result.contains("⏰  `09:30` \\- `10:45`"));
        assertTrue(result.contains("Обсуждение планов на Q1\\-2024"));
        assertTrue(result.contains("*Место\\:* Zoom \\(ссылка в описании\\)"));
    }
}
