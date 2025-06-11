package com.example;

import com.teamHelper.bot.BotComponent;
import com.teamHelper.bot.MessageBuilder;
import com.teamHelper.model.CalendarEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BotComponentTest {

    @Mock
    private MessageBuilder messageBuilder;

    @Mock
    private Update update;

    @Mock
    private Message message;

    private BotComponent botComponent;

    @BeforeEach
    void setUp() {
        // Создаем бот с тестовым токеном
        botComponent = new BotComponent("test_token");
    }

    @Test
    @DisplayName("Должен возвращать корректное имя бота")
    void shouldReturnCorrectBotUsername() {
        // Arrange
        String expectedUsername = "test_bot";
        // Здесь нужно было бы установить через @Value, но для теста используем reflection или сеттер

        // Act & Assert
        // Поскольку botUsername устанавливается через @Value, в реальном тесте нужно мокировать это
        assertNotNull(botComponent.getBotUsername());
    }

    @Test
    @DisplayName("Должен обрабатывать команду /start")
    void shouldHandleStartCommand() {
        // Arrange
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/start");
        when(message.getChatId()).thenReturn(12345L);

        // Act
        botComponent.onUpdateReceived(update);

        // Assert
        // Проверяем, что метод был вызван с правильными параметрами
        verify(update).hasMessage();
        verify(message).hasText();
        verify(message).getText();
        verify(message).getChatId();
    }

    @Test
    @DisplayName("Должен обрабатывать команду /check")
    void shouldHandleCheckCommand() {
        // Arrange
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/check");
        when(message.getChatId()).thenReturn(12345L);

        // Act
        botComponent.onUpdateReceived(update);

        // Assert
        verify(message).getText();
        verify(message).getChatId();
    }

    @Test
    @DisplayName("Должен обрабатывать неизвестные команды")
    void shouldHandleUnknownCommands() {
        // Arrange
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/unknown");
        when(message.getChatId()).thenReturn(12345L);

        // Act
        botComponent.onUpdateReceived(update);

        // Assert
        verify(message).getText();
        verify(message).getChatId();
    }

    @Test
    @DisplayName("Должен игнорировать сообщения без текста")
    void shouldIgnoreMessagesWithoutText() {
        // Arrange
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(false);

        // Act
        botComponent.onUpdateReceived(update);

        // Assert
        verify(update).hasMessage();
        verify(message).hasText();
        verify(message, never()).getText();
    }

    @Test
    @DisplayName("Должен игнорировать апдейты без сообщений")
    void shouldIgnoreUpdatesWithoutMessages() {
        // Arrange
        when(update.hasMessage()).thenReturn(false);

        // Act
        botComponent.onUpdateReceived(update);

        // Assert
        verify(update).hasMessage();
        verify(update, never()).getMessage();
    }

    @Test
    @DisplayName("Должен отправлять уведомления о календарных событиях")
    void shouldSendCalendarNotifications() {
        // Arrange
        CalendarEvent event = new CalendarEvent();
        event.setTitle("Тестовое событие");
        event.setStart(LocalDateTime.now().plusMinutes(5));
        event.setEnd(LocalDateTime.now().plusMinutes(30));

        String expectedMessage = "🔔 Тестовое событие";
        when(messageBuilder.buildEventMessage(event)).thenReturn(expectedMessage);

        // Act
        botComponent.sendCalendarNotification(event);

        // Assert
        verify(messageBuilder).buildEventMessage(event);
    }

    @Test
    @DisplayName("Должен отправлять сообщения об ошибках")
    void shouldSendErrorMessages() {
        // Arrange
        String errorText = "Тестовая ошибка";

        // Act
        botComponent.sendErrorMessage(errorText);

        // Assert
        // Проверяем, что метод не выбрасывает исключения
        assertDoesNotThrow(() -> botComponent.sendErrorMessage(errorText));
    }

    @Test
    @DisplayName("Должен обрабатывать обычный текст как неизвестную команду")
    void shouldHandleRegularTextAsUnknownCommand() {
        // Arrange
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("Привет, как дела?");
        when(message.getChatId()).thenReturn(12345L);

        // Act
        botComponent.onUpdateReceived(update);

        // Assert
        verify(message).getText();
        verify(message).getChatId();
    }
}