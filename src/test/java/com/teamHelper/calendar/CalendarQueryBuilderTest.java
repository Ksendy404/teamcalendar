
package com.teamHelper.calendar;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarQueryBuilderTest {

    @Test
    void shouldBuildXmlBetweenDates() {
        CalendarQueryBuilder builder = new CalendarQueryBuilder();

        LocalDateTime start = LocalDateTime.of(2025, 6, 22, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 6, 22, 18, 0);

        String xml = builder.buildCalendarQuery(start, end);

        assertNotNull(xml);
        assertTrue(xml.contains("DAV:"));
        assertTrue(xml.contains(start.toString().substring(0, 10).replace("-", ""))); // YYYYMMDD
    }
}
