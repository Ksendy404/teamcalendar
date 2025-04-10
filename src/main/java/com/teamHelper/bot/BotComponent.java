package com.teamHelper.bot;

import com.teamHelper.calendar.YandexCalDavService;
import com.teamHelper.pojo.CalendarEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.format.DateTimeFormatter;

@Component
public class BotComponent extends TelegramLongPollingBot {
    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.admin.chatId}")
    private String adminChatId; // ID чата для ошибок

    @Value("${telegram.notification.chatId}")
    private String notificationChatId;

    @Value("${telegram.error.chatId}")
    private String errorChatID;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    public BotComponent(@Value("${telegram.bot.token}") String botToken) {
        super(botToken);
        System.out.println("🟢 Бот инициализирован. Токен: " + botToken.substring(0, 6) + "...");
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleIncomingMessage(update);
        }
    }

    // Отправка сообщения об ошибке администратору
    public void sendErrorMessage(String errorText) {
        SendMessage message = new SendMessage();
        message.setChatId(notificationChatId); // Отправляем в указанный чат
        message.setText("❌ Ошибка: " + errorText);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки: " + e.getMessage());
        }
    }

    // Отправка уведомления о событии
    public void sendCalendarNotification(CalendarEvent event) {
        SendMessage message = new SendMessage();
        message.setChatId(notificationChatId); // Отправляем в указанный чат
        message.setText(buildEventMessage(event));
        message.setParseMode("MarkdownV2");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки: " + e.getMessage());
        }
    }

    // Форматирование сообщения о событии
    private String buildEventMessage(CalendarEvent event) {
        return String.format(
                "🔔 *Напоминание о событии*\n\n" +
                        "*Название\\:* %s\n" +
                        "*Время\\:* `%s` \\- `%s`\n" +
                        "%s" +
                        "%s",
                escapeMarkdownV2(event.getTitle()),
                escapeMarkdownV2(event.getStart().format(DATE_FORMAT)),
                escapeMarkdownV2(event.getEnd().format(DATE_FORMAT)),
                event.getDescription() != null ? "*Описание\\:* " + escapeMarkdownV2(event.getDescription()) + "\n" : "",
                event.getLocation() != null ? "*Место\\:* " + escapeMarkdownV2(event.getLocation().getTitle()) : ""
        );
    }

    // Экранирование символов Markdown
    private String escapeMarkdownV2(String text) {
        if (text == null) return "";
        return text.replaceAll("([_*\\[\\]()~`>#+=|{}\\.!\\-:])", "\\\\$1");
    }

    private void handleIncomingMessage(Update update) {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        switch (messageText) {
            case "/start":
                sendResponse(chatId, "Привет! Я буду напоминать о событиях из календаря.");
                break;
            case "/check":
                sendResponse(chatId, "Проверяю календарь...");
                break;
            default:
                sendResponse(chatId, "Неизвестная команда");
        }
    }

    private void sendResponse(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки: " + e.getMessage());
        }
    }
}