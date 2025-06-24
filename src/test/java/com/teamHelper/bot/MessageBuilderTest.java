package com.teamHelper.bot;

import com.teamHelper.model.CalendarEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MessageBuilderTest {

    private MessageBuilder messageBuilder;

    @BeforeEach
    public void setup() {
        messageBuilder = new MessageBuilder();
    }

    @Test
    public void testBuildEventMessage_basic() {
        CalendarEvent event = new CalendarEvent();
        event.setTitle("Test Event");
        event.setStart(LocalDateTime.of(2025, 6, 23, 10, 0));
        event.setEnd(LocalDateTime.of(2025, 6, 23, 11, 0));

        String message = messageBuilder.buildEventMessage(event);

        assertTrue(message.contains("Test Event"));
        assertTrue(message.contains("10:00"));
        assertTrue(message.contains("11:00"));
    }

    @Test
    public void testBuildEventMessage_withDescription() {
        CalendarEvent event = new CalendarEvent();
        event.setTitle("Event");
        event.setStart(LocalDateTime.now());
        event.setEnd(LocalDateTime.now().plusHours(1));
        event.setDescription("https://test.com");

        String message = messageBuilder.buildEventMessage(event);

        assertTrue(message.contains("https://test.com"));
    }

    @Test
    public void testEscapeMarkdownV2() {
        String escaped = messageBuilder.escapeMarkdownV2("Test *bold* _italic_");
        assertEquals("Test \\*bold\\* \\_italic\\_", escaped);
    }
}