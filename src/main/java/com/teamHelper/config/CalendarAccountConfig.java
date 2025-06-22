package com.teamHelper.config;

import lombok.Data;

@Data
public class CalendarAccountConfig {
    private String id;
    private String url;
    private String username;
    private String password;
    private Long telegramChatId;
}