package com.example;

import com.teamHelper.model.CalendarEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarEventTest {

    @Test
    @DisplayName("Должен создавать пустое событие календаря")
    void shouldCreateEmptyCalendarEvent() {
        CalendarEvent event = new CalendarEvent();

        assertThat(event.getId()).isNull();
        assertThat(event.getTitle()).isNull();
        assertThat(event.getDescription()).isNull();
        assertThat(event.getUrl()).isNull();
        assertThat(event.getStart()).isNull();
        assertThat(event.getEnd()).isNull();
        assertThat(event.getLocation()).isNull();
        assertThat(event.getAttendees()).isNull();
    }

    @Test
    @DisplayName("Должен устанавливать и получать все поля события")
    void shouldSetAndGetAllEventFields() {
        CalendarEvent event = new CalendarEvent();
        LocalDateTime startTime = LocalDateTime.of(2024, 12, 25, 10, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 12, 25, 11, 0);

        event.setId("event123");
        event.setTitle("Встреча команды");
        event.setDescription("Обсуждение проекта");
        event.setUrl("https://zoom.us/meeting");
        event.setStart(startTime);
        event.setEnd(endTime);

        assertThat(event.getId()).isEqualTo("event123");
        assertThat(event.getTitle()).isEqualTo("Встреча команды");
        assertThat(event.getDescription()).isEqualTo("Обсуждение проекта");
        assertThat(event.getUrl()).isEqualTo("https://zoom.us/meeting");
        assertThat(event.getStart()).isEqualTo(startTime);
        assertThat(event.getEnd()).isEqualTo(endTime);
    }

    @Test
    @DisplayName("Должен работать с локацией события")
    void shouldWorkWithEventLocation() {
        CalendarEvent event = new CalendarEvent();
        CalendarEvent.Location location = new CalendarEvent.Location();
        location.setTitle("Переговорная 1");
        location.setUrl("https://maps.google.com/location");

        event.setLocation(location);

        assertThat(event.getLocation()).isNotNull();
        assertThat(event.getLocation().getTitle()).isEqualTo("Переговорная 1");
        assertThat(event.getLocation().getUrl()).isEqualTo("https://maps.google.com/location");
    }

    @Test
    @DisplayName("Должен работать со списком участников")
    void shouldWorkWithAttendeesList() {
        CalendarEvent event = new CalendarEvent();

        CalendarEvent.Attendee attendee1 = new CalendarEvent.Attendee();
        attendee1.setName("Иван Иванов");
        attendee1.setEmail("ivan@example.com");

        CalendarEvent.Attendee attendee2 = new CalendarEvent.Attendee();
        attendee2.setName("Петр Петров");
        attendee2.setEmail("petr@example.com");

        List<CalendarEvent.Attendee> attendees = Arrays.asList(attendee1, attendee2);
        event.setAttendees(attendees);

        assertThat(event.getAttendees()).hasSize(2);
        assertThat(event.getAttendees().get(0).getName()).isEqualTo("Иван Иванов");
        assertThat(event.getAttendees().get(0).getEmail()).isEqualTo("ivan@example.com");
        assertThat(event.getAttendees().get(1).getName()).isEqualTo("Петр Петров");
        assertThat(event.getAttendees().get(1).getEmail()).isEqualTo("petr@example.com");
    }
}