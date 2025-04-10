package com.teamHelper.pojo;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "yandex.caldav")
@Data
public class YandexCalDavProperties {
    private String url;
    private String username;
    private String password;
    private String calendar;
}
