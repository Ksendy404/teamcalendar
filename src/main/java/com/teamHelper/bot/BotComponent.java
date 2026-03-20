package com.teamHelper.bot;

import com.teamHelper.model.CalendarEvent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;

@Slf4j
@Component
public class BotComponent {

    @Autowired
    private MessageBuilder messageBuilder;

    @Value("${TELEGRAM_BOT_TOKEN}")
    private String botToken;

    @Value("${TELEGRAM_BOT_ERROR_CHAT_ID:}")
    private String errorChatId;

    @Value("${PROXY_HOST:}")
    private String proxyHost;

    @Value("${PROXY_PORT:0}")
    private int proxyPort;

    @Value("${PROXY_USER:}")
    private String proxyUser;

    @Value("${PROXY_PASSWORD:}")
    private String proxyPassword;

    private OkHttpClient client;

    @PostConstruct
    public void init() {
        this.client = buildHttpClient();
        log.info("Bot sender initialized");
    }

    private OkHttpClient buildHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(15))
                .readTimeout(Duration.ofSeconds(30))
                .writeTimeout(Duration.ofSeconds(30));

        if (proxyHost != null && !proxyHost.isBlank() && proxyPort > 0) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            builder.proxy(proxy);

            if (proxyUser != null && !proxyUser.isBlank()) {
                builder.proxyAuthenticator((route, response) -> {
                    String credential = Credentials.basic(proxyUser, proxyPassword);
                    return response.request().newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build();
                });
            }
        }

        return builder.build();
    }

    public void sendCalendarNotification(CalendarEvent event, Long chatId) {
        try {
            String text = messageBuilder.buildEventMessage(event);
            sendMessage(chatId.toString(), text);
        } catch (Exception e) {
            log.error("Ошибка отправки уведомления", e);
            sendErrorMessage("Ошибка отправки уведомления: " + e.getMessage());
        }
    }

    public void sendErrorMessage(String text) {
        if (errorChatId == null || errorChatId.isBlank()) {
            return;
        }

        try {
            sendMessage(errorChatId, text);
        } catch (Exception e) {
            log.error("Ошибка при отправке в error chat", e);
        }
    }

    public void sendMessage(String chatId, String text) throws IOException {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        RequestBody body = new FormBody.Builder()
                .add("chat_id", chatId)
                .add("text", text)
                .add("parse_mode", "MarkdownV2")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Telegram API error: HTTP " + response.code() + ", body=" + responseBody);
            }
        }
    }
}