package com.example;

import com.teamHelper.calendar.CalendarQueryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CalendarQueryBuilderTest {

    private CalendarQueryBuilder queryBuilder;

    @BeforeEach
    void setUp() {
        queryBuilder = new CalendarQueryBuilder();
    }

    @Test
    @DisplayName("Должен генерировать корректный CalDAV запрос")
    void shouldBuildCorrectCalendarQuery() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2024, 1, 15, 9, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 15, 18, 0);

        // Act
        String query = queryBuilder.buildCalendarQuery(start, end);

        // Assert
        assertNotNull(query);
        assertTrue(query.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(query.contains("<c:calendar-query"));
        assertTrue(query.contains("xmlns:d=\"DAV:\""));
        assertTrue(query.contains("xmlns:c=\"urn:ietf:params:xml:ns:caldav\""));
        assertTrue(query.contains("<c:time-range"));
        assertTrue(query.contains("VCALENDAR"));
        assertTrue(query.contains("VEVENT"));
    }

    @Test
    @DisplayName("Должен включать корректные временные диапазоны")
    void shouldIncludeCorrectTimeRanges() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2024, 2, 1, 10, 30);
        LocalDateTime end = LocalDateTime.of(2024, 2, 1, 15, 45);

        // Act
        String query = queryBuilder.buildCalendarQuery(start, end);

        // Assert
        // Проверяем, что в запросе есть временные метки
        assertTrue(query.contains("start="));
        assertTrue(query.contains("end="));
        assertTrue(query.contains("20240201T")); // Дата в формате iCalendar
    }

    @Test
    @DisplayName("Должен корректно форматировать время в формат iCalendar")
    void shouldFormatTimeCorrectly() {
        // Arrange
        LocalDateTime dateTime = LocalDateTime.of(2024, 3, 15, 14, 30, 45);

        // Act
        String result = queryBuilder.formatIcalTime(dateTime);

        // Assert
        assertNotNull(result);
        // Проверяем формат: YYYYMMDDTHHMMSSZ
        assertTrue(result.matches("\\d{8}T\\d{6}Z"));
        assertTrue(result.contains("20240315T"));
        assertTrue(result.endsWith("Z")); // UTC timezone
    }

    @Test
    @DisplayName("Должен корректно конвертировать московское время в UTC")
    void shouldConvertMoscowTimeToUtc() {
        // Arrange
        LocalDateTime moscowTime = LocalDateTime.of(2024, 6, 15, 15, 0); // 15:00 MSK

        // Act
        String utcTime = queryBuilder.formatIcalTime(moscowTime);

        // Assert
        assertNotNull(utcTime);
        assertTrue(utcTime.startsWith("20240615T"));
        assertTrue(utcTime.endsWith("Z"));
        // В июне московское время UTC+3, поэтому 15:00 MSK = 12:00 UTC
        assertTrue(utcTime.contains("T12"));
    }

    @Test
    @DisplayName("Должен обрабатывать зимнее время")
    void shouldHandleWinterTime() {
        // Arrange
        LocalDateTime winterTime = LocalDateTime.of(2024, 1, 15, 12, 0); // 12:00 MSK зимой

        // Act
        String utcTime = queryBuilder.formatIcalTime(winterTime);

        // Assert
        assertNotNull(utcTime);
        assertTrue(utcTime.startsWith("20240115T"));
        // В январе московское время UTC+3, поэтому 12:00 MSK = 09:00 UTC
        assertTrue(utcTime.contains("T09"));
    }

    @Test
    @DisplayName("Должен генерировать валидный XML")
    void shouldGenerateValidXml() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 2, 0, 0);

        // Act
        String query = queryBuilder.buildCalendarQuery(start, end);

        // Assert
        assertNotNull(query);
        // Проверяем основные XML элементы
        assertTrue(query.contains("<?xml"));
        assertTrue(query.contains("<c:calendar-query"));
        assertTrue(query.contains("</c:calendar-query>"));
        assertTrue(query.contains("<d:prop>"));
        assertTrue(query.contains("</d:prop>"));
        assertTrue(query.contains("<c:filter>"));
        assertTrue(query.contains("</c:filter>"));

        // Проверяем, что XML корректно закрыт
        long openTags = query.chars().filter(ch -> ch == '<').count();
        long closeTags = query.chars().filter(ch -> ch == '>').count();
        assertEquals(openTags, closeTags);
    }

    @Test
    @DisplayName("Должен обрабатывать граничные даты")
    void shouldHandleBoundaryDates() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2099, 12, 31, 23, 59);

        // Act & Assert
        assertDoesNotThrow(() -> {
            String query = queryBuilder.buildCalendarQuery(start, end);
            assertNotNull(query);
            assertTrue(query.contains("19700101T"));
            assertTrue(query.contains("20991231T"));
        });
    }

    @Test
    @DisplayName("Должен обрабатывать одинаковые даты начала и конца")
    void shouldHandleSameStartAndEndDates() {
        // Arrange
        LocalDateTime sameTime = LocalDateTime.of(2024, 6, 15, 12, 0);

        // Act
        String query = queryBuilder.buildCalendarQuery(sameTime, sameTime);

        // Assert
        assertNotNull(query);
        assertTrue(query.contains("start="));
        assertTrue(query.contains("end="));
        // Проверяем, что обе даты одинаковые
        String formattedTime = queryBuilder.formatIcalTime(sameTime);
        assertTrue(query.contains("start=\"" + formattedTime + "\""));
        assertTrue(query.contains("end=\"" + formattedTime + "\""));
    }
}