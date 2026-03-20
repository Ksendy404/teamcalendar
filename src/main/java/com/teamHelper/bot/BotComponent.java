package com.teamHelper.bot;

import com.teamHelper.model.CalendarEvent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
public class BotComponent extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(BotComponent.class);

    @Autowired
    private MessageBuilder messageBuilder;

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
            System.out.println("Бот инициализирован. Токен не задан или слишком короткий");
        }
    }

    @PostConstruct
    public void register() {
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(this);
            log.info("Бот зарегистрирован в Telegram API");
        } catch (TelegramApiException e) {
            log.error("Ошибка регистрации бота", e);
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    /**
     * Метод для отправки уведомлений в нужный чат
     */
    public void sendCalendarNotification(CalendarEvent event, Long chatId) {
        try {
            String text = messageBuilder.buildEventMessage(event);
            SendMessage message = new SendMessage(chatId.toString(), text);
            message.enableHtml(true);
            message.setParseMode("MarkdownV2");
            execute(message);
        } catch (TelegramApiException e) {
            sendErrorMessage("Ошибка отправки уведомления: " + e.getMessage());
        }
    }

    public void sendErrorMessage(String text) {
        if (errorChatId != null) {
            try {
                SendMessage message = new SendMessage(errorChatId, text);
                execute(message);
            } catch (TelegramApiException e) {
                System.err.println("Ошибка при отправке сообщения в чат ошибок: " + e.getMessage());
            }
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
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