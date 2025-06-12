package com.teamHelper.bot;

import com.teamHelper.model.CalendarEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class BotComponent extends TelegramLongPollingBot {

    @Autowired
    private MessageBuilder messageBuilder;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.admin.chatId}")
    private String adminChatId;

    @Value("${telegram.notification.chatId}")
    private String notificationChatId;

    @Value("${telegram.error.chatId}")
    private String errorChatID;

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

    // Отправка сообщения об ошибке администраторам
    public void sendErrorMessage(String errorText) {
        SendMessage message = new SendMessage();
        message.setChatId(errorChatID);
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
        message.setText(messageBuilder.buildEventMessage(event));
        message.setParseMode("MarkdownV2");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки: " + e.getMessage());
        }
    }

    private void handleIncomingMessage(Update update) {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        String helpText = """
                Я маленький и такой команды пока что незнаю
                               
                ⏰ Вот мои доступные команды:
                /check Проверить общие события на сегодня
                """;

        switch (messageText) {
            case "/start":
                sendResponse(chatId, "Привет! Я напоминаю о событиях календаря команде");
                break;
            case "/check":
                sendResponse(chatId, "Проверяю календарь...");

                break;
            default:
                sendResponse(chatId, helpText);
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