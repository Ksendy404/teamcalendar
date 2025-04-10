package com.example;

import com.teamHelper.calendar.YandexCalDavService;
import lombok.SneakyThrows;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class YandexCalDavServiceTest {
    private YandexCalDavService calDavService;

    @BeforeClass
    public void setUp() {
        // Инициализация с тестовыми данными
        calDavService = new YandexCalDavService(
                "https://caldav.yandex.ru/calendars/itTeamHelper@yandex.com/events-32878960/",
                "itTeamHelper@yandex.com",
                "qjwmrfffpsxlskou"
        );
    }
    @SneakyThrows
    @Test
    public void testConnection() {
        System.out.println("=== Testing connection ===");

            String result = calDavService.testCalDavConnection();
            System.out.println(result);
            assertTrue(result.contains("SUCCESS"), "Connection should succeed");

    }
}