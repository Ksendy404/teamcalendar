/*
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
*/
package com.teamHelper.bot;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j
@Component
public class TelegramConnectivityChecker {

    @PostConstruct
    public void checkConnection() {
        try {
            URL url = new URL("https://api.telegram.org");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int code = connection.getResponseCode();
            log.info("Проверка соединения с Telegram: {}", code == 200 ? "OK" : "FAIL (" + code + ")");
        } catch (Exception e) {
            log.warn("Проверка соединения с Telegram не пройдена: {}", e.getMessage());
        }
    }
}