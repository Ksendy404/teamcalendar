package com.teamHelper.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
@Configuration
public class CalendarAccountsProperties {

    @Value("${CALDAV_USERNAME}")
    private String sharedUsername;

    @Value("${CALDAV_PASS}")
    private String sharedPassword;

    @Value("${CALENDAR_COUNT}")
    private int calendarCount;

    @Value("#{systemEnvironment}")
    private Map<String, String> env;

    private List<CalendarAccountConfig> accounts = new ArrayList<>();

    @PostConstruct
    public void loadAccounts() {
        for (int i = 1; i <= calendarCount; i++) {
            String prefix = "CALENDAR_" + i + "_";

            CalendarAccountConfig config = new CalendarAccountConfig();
            config.setId(env.get(prefix + "ID"));
            config.setUrl(env.get(prefix + "URL"));
            config.setUsername(sharedUsername);
            config.setPassword(sharedPassword);
            config.setTelegramChatId(Long.parseLong(env.get(prefix + "CHAT_ID")));

            accounts.add(config);
            log.info("✅ Загружена конфигурация календаря {} → чат {}", config.getId(), config.getTelegramChatId());
        }
    }
}