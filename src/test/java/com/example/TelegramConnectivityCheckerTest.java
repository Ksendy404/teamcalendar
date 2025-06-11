package com.example;

import com.teamHelper.bot.TelegramConnectivityChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@DisplayName("Тесты для TelegramConnectivityChecker")
class TelegramConnectivityCheckerTest {

    private TelegramConnectivityChecker checker;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        checker = new TelegramConnectivityChecker();
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
    }

    @Test
    @DisplayName("Должен выводить OK при успешном соединении")
    void shouldPrintOkOnSuccessfulConnection() throws Exception {
        try (MockedStatic<URL> mockedUrl = Mockito.mockStatic(URL.class)) {
            URL mockUrl = mock(URL.class);
            HttpURLConnection mockConnection = mock(HttpURLConnection.class);

            mockedUrl.when(() -> new URL("https://api.telegram.org")).thenReturn(mockUrl);
            when(mockUrl.openConnection()).thenReturn(mockConnection);
            when(mockConnection.getResponseCode()).thenReturn(200);

            checker.checkConnection();

            String output = outputStream.toString();
            assertThat(output).contains("Проверка соединения с Telegram: OK");

            verify(mockConnection).setRequestMethod("GET");
        }
    }

    @Test
    @DisplayName("Должен выводить FAIL при неуспешном соединении")
    void shouldPrintFailOnUnsuccessfulConnection() throws Exception {
        try (MockedStatic<URL> mockedUrl = Mockito.mockStatic(URL.class)) {
            URL mockUrl = mock(URL.class);
            HttpURLConnection mockConnection = mock(HttpURLConnection.class);

            mockedUrl.when(() -> new URL("https://api.telegram.org")).thenReturn(mockUrl);
            when(mockUrl.openConnection()).thenReturn(mockConnection);
            when(mockConnection.getResponseCode()).thenReturn(500);

            checker.checkConnection();

            String output = outputStream.toString();
            assertThat(output).contains("Проверка соединения с Telegram: FAIL");
        }
    }

    @Test
    @DisplayName("Должен пробрасывать исключение при ошибке соединения")
    void shouldThrowExceptionOnConnectionError() throws Exception {
        try (MockedStatic<URL> mockedUrl = Mockito.mockStatic(URL.class)) {
            URL mockUrl = mock(URL.class);

            mockedUrl.when(() -> new URL("https://api.telegram.org")).thenReturn(mockUrl);
            when(mockUrl.openConnection()).thenThrow(new RuntimeException("Network error"));

            assertThrows(Exception.class, () -> checker.checkConnection());
        }
    }

    void tearDown() {
        System.setOut(originalOut);
    }
}
