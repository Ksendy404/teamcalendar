package com.teamHelper.calendar;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class CalendarQueryBuilder {

    //Строит CalDAV запрос для указанного временного диапазона
    public String buildCalendarQuery(LocalDateTime start, LocalDateTime end) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <c:calendar-query xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav">
                  <d:prop>
                    <d:getetag/>
                    <c:calendar-data/>
                  </d:prop>
                  <c:filter>
                    <c:comp-filter name="VCALENDAR">
                      <c:comp-filter name="VEVENT">
                        <c:time-range start="%s" end="%s"/>
                      </c:comp-filter>
                    </c:comp-filter>
                  </c:filter>
                </c:calendar-query>
                """.formatted(
                formatIcalTime(start),
                formatIcalTime(end)
        );
    }

    //Форматирует LocalDateTime в iCalendar формат (UTC)
    public String formatIcalTime(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.of("Europe/Moscow"))
                .withZoneSameInstant(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
    }
}
