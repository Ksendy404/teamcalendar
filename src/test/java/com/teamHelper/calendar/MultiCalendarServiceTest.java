package com.teamHelper.calendar;

import com.teamHelper.config.CalendarAccountConfig;
import com.teamHelper.config.CalendarAccountsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultiCalendarServiceTest {

    private CalendarAccountsProperties calendarAccountsProperties;
    private MultiCalendarService service;

    @BeforeEach
    void setup() {
        calendarAccountsProperties = mock(CalendarAccountsProperties.class);
        service = new MultiCalendarService(calendarAccountsProperties);
    }

    @Test
    void shouldReturnEmptyListIfNoAccounts() {
        when(calendarAccountsProperties.getAccounts()).thenReturn(Collections.emptyList());
        List<MultiCalendarService.EventWithChat> events = service.getAllEventsForToday();
        assertTrue(events.isEmpty());
    }

    @Test
    void shouldHandleCalendarExceptionGracefully() {
        CalendarAccountConfig account = new CalendarAccountConfig();
        account.setId("acc1");
        account.setUrl("http://invalid-url");
        account.setUsername("user");
        account.setPassword("pass");
        account.setTelegramChatId(123L);

        when(calendarAccountsProperties.getAccounts()).thenReturn(List.of(account));

        List<MultiCalendarService.EventWithChat> events = service.getAllEventsForToday();
        assertNotNull(events);
    }
}
