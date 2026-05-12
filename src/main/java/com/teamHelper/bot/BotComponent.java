package com.teamHelper.bot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamHelper.model.CalendarEvent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class BotComponent {

    @Autowired
    private MessageBuilder messageBuilder;

    @Value("${TELEGRAM_BOT_TOKEN}")
    private String botToken;

    @Value("${MM_URL}")
    private String mmUrl;

    @Value("${MM_BOT_TOKEN}")
    private String mmToken;

    @Value("${TELEGRAM_BOT_ERROR_CHAT_ID:}")
    private String errorChatId;

    @Value("${MM_BOT_ERROR_CHAT_ID:}")
    private String errorChatIdMm;

    @Value("${PROXY_HOST:}")
    private String proxyHost;

    @Value("${PROXY_PORT:0}")
    private int proxyPort;

    @Value("${PROXY_USER:}")
    private String proxyUser;

    @Value("${PROXY_PASSWORD:}")
    private String proxyPassword;

    private OkHttpClient client;

    private final ObjectMapper objectMapper = new ObjectMapper();

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

    public void sendCalendarNotification(CalendarEvent event, Long chatId, String mmChatId) {
        try {
            String text = messageBuilder.buildEventMessage(event);
            sendMessage(chatId.toString(), text);
        } catch (Exception e) {
            log.error("Ошибка отправки уведомления в Telegram", e);
            sendErrorMessage("Ошибка отправки уведомления в Telegram: " + e.getMessage() + " в " + chatId);
        }

        try {
            String text = messageBuilder.buildEventMessage(event);
            sendMessageToMattermost(mmChatId, text);
        } catch (Exception e) {
            log.error("Ошибка отправки уведомления в Mattermost", e);
            sendErrorMessage("Ошибка отправки уведомления в Mattermost: " + e.getMessage());
        }
    }

    public void sendErrorMessage(String text) {
        if (errorChatId == null || errorChatId.isBlank() || errorChatIdMm == null || errorChatIdMm.isBlank()) {
            return;
        }

        try {
            sendMessage(errorChatId, text);
        } catch (Exception e) {
            log.error("Ошибка при отправке в error chat Telegram", e);
        }

        try {
            sendMessageToMattermost(errorChatIdMm, text);
        } catch (Exception e) {
            log.error("Ошибка при отправке в error chat Mattermost", e);
        }
    }

    public void sendMessage(String chatId, String text) throws IOException {
        log.info("Telegram sendMessage chatId=[{}]", chatId);

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

    public void sendMessageToMattermost(String chatId, String text) throws IOException {
        log.info("Telegram sendMessage chatId=[{}]", chatId);

        String url = mmUrl;

        String cleanedText = text
                .replace("\\=", "=")
                .replace("\\.", ".")
                .replace("\\_", "_")
                .replace("\\*", "*")
                .replace("\\[", "[")
                .replace("\\]", "]")
                .replace("\\(", "(")
                .replace("\\)", ")")
                .replace("\\n", "\n  ");

        Map<String, String> payload = new HashMap<>();
        payload.put("channel_id", chatId);
        payload.put("message", cleanedText);

        String jsonBody = objectMapper.writeValueAsString(payload);

        RequestBody body = RequestBody.create(
                jsonBody,
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + mmToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Mattermost API error: HTTP " + response.code() + ", body=" + responseBody);
            }
        }
    }
}