package com.teamHelper.calendar;

import com.teamHelper.config.CalendarAccountConfig;
import com.teamHelper.config.CalendarAccountsProperties;
import com.teamHelper.model.CalendarEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiCalendarService {

    private final CalendarAccountsProperties accountsProperties;

    /**
     * Обходит все календари и возвращает события с привязкой к chatId
     */
    public List<EventWithChat> getAllEventsForToday() {
        List<EventWithChat> result = new ArrayList<>();

        for (CalendarAccountConfig account : accountsProperties.getAccounts()) {
            try {
                log.debug("🔗 Подключаюсь к календарю {}", account.getId());

                YandexCalDavService service = new YandexCalDavService(
                        account.getUrl(),
                        account.getUsername(),
                        account.getPassword()
                );

                List<CalendarEvent> events = service.getUpcomingEvents();

                for (CalendarEvent event : events) {
                    result.add(new EventWithChat(event, account.getTelegramChatId()));
                }

                log.info("✅ Получено {} событий из календаря {}", events.size(), account.getId());

            } catch (Exception e) {
                log.error("❌ Ошибка при получении событий из календаря {}: {}", account.getId(), e.getMessage());
            }
        }

        return result;
    }

    /**
     * Обёртка для события с Telegram-чатом
     */
    public record EventWithChat(CalendarEvent event, Long chatId) {}
}