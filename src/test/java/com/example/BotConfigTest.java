package com.example;

import com.teamHelper.bot.BotComponent;
import com.teamHelper.bot.BotConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для BotConfig")
class BotConfigTest {

    @Mock
    private BotComponent botComponent;

    private BotConfig botConfig = new BotConfig();

    @Test
    @DisplayName("Должен создавать и регистрировать TelegramBotsApi")
    void shouldCreateAndRegisterTelegramBotsApi() throws TelegramApiException {
        try (MockedStatic<TelegramBotsApi> mockedApi = mockStatic(TelegramBotsApi.class)) {
            TelegramBotsApi mockApi = mock(TelegramBotsApi.class);
            mockedApi.when(() -> new TelegramBotsApi(DefaultBotSession.class)).thenReturn(mockApi);

            TelegramBotsApi result = botConfig.telegramBotsApi(botComponent);

            assertThat(result).isEqualTo(mockApi);
            verify(mockApi).registerBot(botComponent);
        }
    }

    @Test
    @DisplayName("Должен пробрасывать TelegramApiException при ошибке регистрации")
    void shouldThrowTelegramApiExceptionOnRegistrationError() throws TelegramApiException {
        try (MockedStatic<TelegramBotsApi> mockedApi = mockStatic(TelegramBotsApi.class)) {
            TelegramBotsApi mockApi = mock(TelegramBotsApi.class);
            mockedApi.when(() -> new TelegramBotsApi(DefaultBotSession.class)).thenReturn(mockApi);
            doThrow(new TelegramApiException("Registration failed")).when(mockApi).registerBot(any());

            assertThrows(TelegramApiException.class, () -> botConfig.telegramBotsApi(botComponent));
        }
    }
}

