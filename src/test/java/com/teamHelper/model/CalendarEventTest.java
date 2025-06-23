
package com.teamHelper.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CalendarEventTest {

    @Test
    void shouldCreateEventWithAllFields() {
        CalendarEvent event = new CalendarEvent();
        event.setId("evt-1");
        event.setTitle("Team Sync");
        event.setDescription("Обсуждение задач");
        event.setUrl("http://example.com");
        event.setStart(LocalDateTime.of(2025, 6, 22, 9, 0));
        event.setEnd(LocalDateTime.of(2025, 6, 22, 10, 0));

        CalendarEvent.Location location = new CalendarEvent.Location();
        location.setTitle("Telemost");
        event.setLocation(location);

        assertEquals("evt-1", event.getId());
        assertEquals("Team Sync", event.getTitle());
        assertEquals("Обсуждение задач", event.getDescription());
        assertEquals("http://example.com", event.getUrl());
        assertEquals("Telemost", event.getLocation().getTitle());
        assertEquals(9, event.getStart().getHour());
        assertEquals(10, event.getEnd().getHour());
    }

    @Test
    void shouldAllowEmptyFields() {
        CalendarEvent event = new CalendarEvent();
        assertNull(event.getId());
        assertNull(event.getTitle());
        assertNull(event.getStart());
    }
}
