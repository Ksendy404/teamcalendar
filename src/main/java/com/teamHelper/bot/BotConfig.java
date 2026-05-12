package com.teamHelper.bot;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Slf4j
@Configuration
public class BotConfig {

    @Value("${TELEGRAM_BOT_TOKEN}")
    private String token;

    @Value("${TELEGRAM_BOT_USERNAME}")
    private String username;

    @Value("${TELEGRAM_BOT_ADMIN_CHAT_ID}")
    private String adminChatId;

    @Value("${TELEGRAM_BOT_ERROR_CHAT_ID}")
    private String errorChatId;

    @Value("${TELEGRAM_BOT_NOTIFICATION_CHAT_ID:}")
    private String defaultNotificationChatId;

    @Value("${MM_URL}")
    private String mmUrl;

    @Value("${MM_BOT_TOKEN}")
    private String mmToken;

    @Value("${MM_BOT_ADMIN_CHAT_ID}")
    private String adminChatIdMm;

    @Value("${MM_BOT_ERROR_CHAT_ID}")
    private String errorChatIdMm;

    @Value("${MM_BOT_NOTIFICATION_CHAT_ID:}")
    private String defaultNotificationChatIdMm;
}