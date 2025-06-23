package com.teamHelper.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@Getter
public class CalendarAccountsProperties {

    private final List<CalendarAccountConfig> accounts = new ArrayList<>();

    @PostConstruct
    public void init() {
        int count = Integer.parseInt(Optional.ofNullable(System.getenv("CALENDAR_COUNT")).orElse("0"));

        for (int i = 1; i <= count; i++) {
            String id = System.getenv("CALENDAR_" + i + "_ID");
            String url = System.getenv("CALENDAR_" + i + "_URL");
            String chatIdStr = System.getenv("CALENDAR_" + i + "_CHAT_ID");

            if (id == null || url == null || chatIdStr == null) {
                log.warn("⚠️ Пропущена конфигурация календаря #{}", i);
                continue;
            }

            Long chatId;
            try {
                chatId = Long.parseLong(chatIdStr);
            } catch (NumberFormatException e) {
                log.warn("⚠️ Неверный формат CHAT_ID для календаря {}: {}", id, chatIdStr);
                continue;
            }

            CalendarAccountConfig config = new CalendarAccountConfig(id, url, chatId);
            accounts.add(config);

            log.info("✅ Загружена конфигурация календаря {} → чат {}", id, chatId);
        }
    }
}