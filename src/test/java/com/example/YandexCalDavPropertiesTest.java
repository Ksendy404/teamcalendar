package com.example;

import com.teamHelper.model.YandexCalDavProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Тесты для YandexCalDavProperties")
class YandexCalDavPropertiesTest {

    @Test
    @DisplayName("Должен создавать объект с пустыми полями по умолчанию")
    void shouldCreateEmptyPropertiesObject() {
        YandexCalDavProperties properties = new YandexCalDavProperties();

        assertThat(properties.getUrl()).isNull();
        assertThat(properties.getUsername()).isNull();
        assertThat(properties.getPassword()).isNull();
        assertThat(properties.getCalendar()).isNull();
    }

    @Test
    @DisplayName("Должен устанавливать и получать URL")
    void shouldSetAndGetUrl() {
        YandexCalDavProperties properties = new YandexCalDavProperties();
        String expectedUrl = "https://caldav.yandex.ru/calendars/test@yandex.ru/events";

        properties.setUrl(expectedUrl);

        assertThat(properties.getUrl()).isEqualTo(expectedUrl);
    }

    @Test
    @DisplayName("Должен устанавливать и получать имя пользователя")
    void shouldSetAndGetUsername() {
        YandexCalDavProperties properties = new YandexCalDavProperties();
        String expectedUsername = "test@yandex.ru";

        properties.setUsername(expectedUsername);

        assertThat(properties.getUsername()).isEqualTo(expectedUsername);
    }

    @Test
    @DisplayName("Должен устанавливать и получать пароль")
    void shouldSetAndGetPassword() {
        YandexCalDavProperties properties = new YandexCalDavProperties();
        String expectedPassword = "secret123";

        properties.setPassword(expectedPassword);

        assertThat(properties.getPassword()).isEqualTo(expectedPassword);
    }

    @Test
    @DisplayName("Должен устанавливать и получать календарь")
    void shouldSetAndGetCalendar() {
        YandexCalDavProperties properties = new YandexCalDavProperties();
        String expectedCalendar = "personal";

        properties.setCalendar(expectedCalendar);

        assertThat(properties.getCalendar()).isEqualTo(expectedCalendar);
    }

    @Test
    @DisplayName("Должен корректно работать equals и hashCode")
    void shouldWorkWithEqualsAndHashCode() {
        YandexCalDavProperties properties1 = new YandexCalDavProperties();
        properties1.setUrl("https://test.com");
        properties1.setUsername("user");
        properties1.setPassword("pass");
        properties1.setCalendar("cal");

        YandexCalDavProperties properties2 = new YandexCalDavProperties();
        properties2.setUrl("https://test.com");
        properties2.setUsername("user");
        properties2.setPassword("pass");
        properties2.setCalendar("cal");

        assertThat(properties1).isEqualTo(properties2);
        assertThat(properties1.hashCode()).isEqualTo(properties2.hashCode());
    }
}
