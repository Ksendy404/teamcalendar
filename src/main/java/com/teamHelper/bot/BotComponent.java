package com.teamHelper.bot;

import com.teamHelper.model.CalendarEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import jakarta.annotation.PostConstruct;

@Component
@Slf4j
public class BotComponent extends TelegramLongPollingBot {

    @Value("${TELEGRAM_BOT_USERNAME}")
    private String botUsername;

    @Value("${TELEGRAM_BOT_TOKEN}")
    private String botToken;

    @Value("${TELEGRAM_BOT_ADMIN_CHAT_ID}")
    private String adminChatId;

    @Value("${TELEGRAM_BOT_ERROR_CHAT_ID}")
    private String errorChatId;

    public BotComponent(@Value("${telegram.bot.token:}") String botToken) {
        super(botToken);
        if (botToken != null && botToken.length() >= 6) {
            System.out.println("🟢 Бот инициализирован. Токен: " + botToken.substring(0, 6) + "...");
        } else {
            System.out.println("⚠️ Бот инициализирован. Токен не задан или слишком короткий");
        }
    }

    @PostConstruct
    public void register() {
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(this);
            log.info("✅ Бот зарегистрирован в Telegram API");
        } catch (TelegramApiException e) {
            log.error("❌ Ошибка регистрации бота: {}", e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    /**
     * Новый метод для отправки уведомлений в нужный чат
     */
    public void sendCalendarNotification(CalendarEvent event, Long chatId) {
        String text = "📅 *" + event.getTitle() + "*\n" +
                "🕒 " + event.getStart().toString();

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.enableMarkdown(true);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("❌ Ошибка отправки уведомления в чат {}: {}", chatId, e.getMessage());
        }
    }

    public void sendErrorMessage(String error) {
        SendMessage message = new SendMessage();
        message.setChatId(errorChatId);
        message.setText("❗ Ошибка: " + error);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("❌ Ошибка отправки сообщения об ошибке: {}", e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleIncomingMessage(update);
        }
    }

    private void handleIncomingMessage(Update update) {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        String helpText = """
            Я маленький и такой команды пока что не знаю.
            Доступные команды:
            /start — приветствие
            """;

        switch (messageText) {
            case "/start" -> sendResponse(chatId, "Привет! Я напоминаю о событиях календаря команде.");
            default -> sendResponse(chatId, helpText);
        }
    }

    private void sendResponse(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки ответа: {}", e.getMessage());
        }
    }
}