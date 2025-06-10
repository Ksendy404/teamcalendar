package com.teamHelper.bot;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;

@Component
public class TelegramConnectivityChecker {
    @PostConstruct
    public void checkConnection() throws Exception {
        URL url = new URL("https://api.telegram.org");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        int code = connection.getResponseCode();
        System.out.println("Проверка соединения с Telegram: " + (code == 200 ? "OK" : "FAIL"));
    }
}
