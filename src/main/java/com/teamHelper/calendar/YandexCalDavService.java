package com.teamHelper.calendar;

import com.teamHelper.config.CalendarAccountConfig;
import com.teamHelper.config.HttpPropfind;
import com.teamHelper.config.HttpReport;
import com.teamHelper.model.CalendarEvent;
import jakarta.annotation.PreDestroy;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.RecurrenceId;
import org.apache.commons.io.IOUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class YandexCalDavService {

    private static final Logger log = LoggerFactory.getLogger(YandexCalDavService.class);

    private final String caldavUrl;
    private final String username;
    private final String password;
    private final CloseableHttpClient client;
    private ZoneId zoneId;

    @Autowired
    private CalendarQueryBuilder calendarQueryBuilder;

    public YandexCalDavService(
            @Value("${yandex.caldav.url}") String caldavUrl,
            @Value("${yandex.caldav.username}") String username,
            @Value("${yandex.caldav.password}") String password) {
        this.caldavUrl = caldavUrl;
        this.username = username;
        this.password = password;
        this.zoneId = ZoneId.of("Europe/Moscow");

        // Настройка RequestConfig для игнорирования cookie ошибок
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(30000)
                .setSocketTimeout(30000)
                .setConnectionRequestTimeout(30000)
                .build();

        this.client = HttpClients.custom()
                .setDefaultCredentialsProvider(new BasicCredentialsProvider() {{
                    setCredentials(AuthScope.ANY,
                            new UsernamePasswordCredentials(username, password));
                }})
                .setDefaultRequestConfig(requestConfig)
                // Отключаем обработку cookies для избежания предупреждений
                .disableCookieManagement()
                .build();
    }

    public List<CalendarEvent> getUpcomingEvents(CalendarAccountConfig account) throws Exception {
        try {
            LocalDateTime start = LocalDate.now().atTime(CalendarConstants.WORK_START);
            LocalDateTime end = LocalDate.now().atTime(CalendarConstants.WORK_END);

            log.debug("Загружаю события из календаря {} → {}", account.getId(), account.getUrl());

            HttpReport request = new HttpReport(URI.create(account.getUrl()));
            request.setHeader("Depth", "1");
            request.setHeader("Content-Type", "text/xml; charset=utf-8");
            request.setHeader("Prefer", "return-minimal");

            String xmlBody = calendarQueryBuilder.buildCalendarQuery(start, end);
            request.setEntity(new StringEntity(xmlBody, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode != 207) {
                    log.error("Ошибка CalDAV: HTTP {}", statusCode);
                    throw new RuntimeException(String.format("Ошибка CalDAV %d", statusCode));
                }

                List<CalendarEvent> events = parseEvents(new ByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8)));

                return events;
            }

        } catch (Exception e) {
            log.error("Ошибка получения событий: {}", e.getMessage());
            throw new RuntimeException("Ошибка получения событий календаря", e);
        }
    }

    private List<CalendarEvent> parseEvents(InputStream icalStream) throws Exception {
        String xmlResponse = IOUtils.toString(icalStream, StandardCharsets.UTF_8);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document doc;
        try {
            String cleanXml = xmlResponse.replace("\uFEFF", "").trim();
            doc = builder.parse(new InputSource(new StringReader(cleanXml)));
        } catch (Exception e) {
            log.error("Ошибка парсинга XML: {}", e.getMessage());
            throw new RuntimeException("Ошибка парсинга XML: " + e.getMessage(), e);
        }

        if (doc == null || doc.getDocumentElement() == null) {
            throw new RuntimeException("Неверная структура XML документа");
        }

        NodeList responseNodes = doc.getElementsByTagNameNS("DAV:", "response");
        List<CalendarEvent> events = new ArrayList<>();
        CalendarBuilder calendarBuilder = new CalendarBuilder();

        LocalDateTime periodStart = LocalDateTime.now().minusDays(1);
        LocalDateTime periodEnd = LocalDateTime.now().plusDays(1);

        for (int i = 0; i < responseNodes.getLength(); i++) {
            Node responseNode = responseNodes.item(i);
            NodeList calendarDataNodes = ((Element) responseNode)
                    .getElementsByTagNameNS("urn:ietf:params:xml:ns:caldav", "calendar-data");

            for (int j = 0; j < calendarDataNodes.getLength(); j++) {
                String icalContent = calendarDataNodes.item(j).getTextContent();
                try {
                    icalContent = icalContent.trim()
                            .replaceAll("\r", "")
                            .replaceAll("\n ", "\n");

                    if (icalContent.isEmpty()) continue;

                    Calendar calendar = calendarBuilder.build(new StringReader(icalContent));

                    // Разворачиваем повторяющиеся события
                    List<CalendarEvent> blockEvents = expandRecurringEvents(calendar, periodStart, periodEnd);
                    events.addAll(blockEvents);

                } catch (Exception e) {
                    log.warn("Ошибка парсинга iCalendar блока #{}-{}: {}", i + 1, j + 1, e.getMessage());
                }
            }
        }

        return events;
    }

    private List<CalendarEvent> expandRecurringEvents(Calendar calendar, LocalDateTime periodStart, LocalDateTime periodEnd) {
        List<CalendarEvent> expandedEvents = new ArrayList<>();

        ZoneId zoneId = ZoneId.of("Europe/Moscow");
        DateTime icalStart = new DateTime(Date.from(periodStart.atZone(zoneId).toInstant()));
        DateTime icalEnd = new DateTime(Date.from(periodEnd.atZone(zoneId).toInstant()));
        Period period = new Period(icalStart, icalEnd);

        Map<String, VEvent> overrideMap = new HashMap<>();
        for (Object component : calendar.getComponents(Component.VEVENT)) {
            VEvent vEvent = (VEvent) component;
            // Ищем события с RECURRENCE-ID – это перенесённые/изменённые экземпляры
            if (vEvent.getProperty(Property.RECURRENCE_ID) != null) {
                RecurrenceId recId = (RecurrenceId) vEvent.getProperty(Property.RECURRENCE_ID);
                java.util.Date recDate = recId.getDate();
                if (recDate != null) {
                    String baseUid = vEvent.getUid().getValue();
                    String key = baseUid + "_" + recDate.getTime();
                    overrideMap.put(key, vEvent);
                }
                // Добавляем переопределённое событие как отдельное
                try {
                    CalendarEvent overrideEvent = convertEvent(vEvent);
                    expandedEvents.add(overrideEvent);
                } catch (Exception e) {
                    log.warn("Ошибка конвертации переопределённого события '{}': {}",
                            vEvent.getSummary() != null ? vEvent.getSummary().getValue() : "Без названия",
                            e.getMessage());
                }
            }
        }

        // Теперь обрабатываем все события, включая повторяющиеся (без RECURRENCE-ID)
        for (Object component : calendar.getComponents(Component.VEVENT)) {
            VEvent vEvent = (VEvent) component;

            // Пропускаем переопределённые события
            if (vEvent.getProperty(Property.RECURRENCE_ID) != null) {
                continue;
            }

            try {
                // Если есть правило повторения, разворачиваем его
                if (vEvent.getProperty(Property.RRULE) != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Разворачиваем повторяющееся событие: '{}'",
                                vEvent.getSummary() != null ? vEvent.getSummary().getValue() : "Без названия");
                    }
                    // Получаем все экземпляры события в указанном периоде
                    PeriodList periodList = vEvent.calculateRecurrenceSet(period);
                    for (Object periodObj : periodList) {
                        Period eventPeriod = (Period) periodObj;
                        // Проверяем, не переопределён ли этот экземпляр
                        String overrideKey = vEvent.getUid().getValue() + "_" +
                                ((DateTime) eventPeriod.getStart()).getTime();
                        if (overrideMap.containsKey(overrideKey)) {
                            // Этот экземпляр заменён событием с RECURRENCE-ID, пропускаем оригинал
                            continue;
                        }
                        // Создаём копию события для каждого повторения
                        CalendarEvent expandedEvent = convertEventWithCustomTime(
                                vEvent,
                                eventPeriod.getStart(),
                                eventPeriod.getEnd());
                        expandedEvents.add(expandedEvent);
                    }
                } else {
                    // Обычное событие без повторений (и без RECURRENCE-ID)
                    CalendarEvent singleEvent = convertEvent(vEvent);
                    expandedEvents.add(singleEvent);
                }
            } catch (Exception e) {
                log.warn("Ошибка разворачивания события '{}': {}",
                        vEvent.getSummary() != null ? vEvent.getSummary().getValue() : "Без названия",
                        e.getMessage());
                // В случае ошибки добавляем событие как обычное
                try {
                    CalendarEvent fallbackEvent = convertEvent(vEvent);
                    expandedEvents.add(fallbackEvent);
                } catch (Exception e2) {
                    log.error("Критическая ошибка конвертации события: {}", e2.getMessage());
                }
            }
        }

        return expandedEvents;
    }

    private CalendarEvent convertEventWithCustomTime(VEvent vEvent, DateTime startTime,
                                                     DateTime endTime) {
        ZoneId serverZone = ZoneId.of("Europe/Moscow");

        String title = vEvent.getSummary() != null ? vEvent.getSummary().getValue() : "Без названия";

        CalendarEvent event = new CalendarEvent();

        // Создаем уникальный ID для каждого повторения
        String baseId = vEvent.getUid().getValue();
        String uniqueId = baseId + "_" + startTime.getTime();
        event.setId(uniqueId);

        event.setTitle(title);

        // Используем переданное время вместо времени из оригинального события
        if (startTime != null) {
            LocalDateTime startDateTime = startTime.toInstant()
                    .atZone(serverZone)
                    .toLocalDateTime();
            event.setStart(startDateTime);
        }

        if (endTime != null) {
            LocalDateTime endDateTime = endTime.toInstant()
                    .atZone(serverZone)
                    .toLocalDateTime();
            event.setEnd(endDateTime);
        }

        if (vEvent.getDescription() != null) {
            event.setDescription(vEvent.getDescription().getValue());
        }

        if (vEvent.getUrl() != null) {
            event.setUrl(vEvent.getUrl().getValue());
        }

        if (vEvent.getLocation() != null) {
            CalendarEvent.Location location = new CalendarEvent.Location();
            location.setTitle(vEvent.getLocation().getValue());
            event.setLocation(location);
        }

        return event;
    }

    private CalendarEvent convertEvent(VEvent vEvent) {
        ZoneId serverZone = ZoneId.of("Europe/Moscow");

        String title = vEvent.getSummary() != null ? vEvent.getSummary().getValue() : "Без названия";

        CalendarEvent event = new CalendarEvent();

        event.setId(vEvent.getUid().getValue());
        event.setTitle(title);

        if (vEvent.getStartDate() != null && vEvent.getStartDate().getDate() != null) {
            event.setStart(vEvent.getStartDate().getDate().toInstant()
                    .atZone(serverZone)
                    .toLocalDateTime());
        }

        if (vEvent.getEndDate() != null && vEvent.getEndDate().getDate() != null) {
            event.setEnd(vEvent.getEndDate().getDate().toInstant()
                    .atZone(serverZone)
                    .toLocalDateTime());
        }

        if (vEvent.getDescription() != null) {
            event.setDescription(vEvent.getDescription().getValue());
        }

        if (vEvent.getUrl() != null) {
            event.setUrl(vEvent.getUrl().getValue());
        }

        if (vEvent.getLocation() != null) {
            CalendarEvent.Location location = new CalendarEvent.Location();
            location.setTitle(vEvent.getLocation().getValue());
            event.setLocation(location);
        }

        return event;
    }

    @PreDestroy
    public void close() throws IOException {
        client.close();
    }

    public void testCalDavConnection() throws Exception {
        log.debug("Тестирование CalDAV подключения к {}", caldavUrl);

        HttpPropfind request = new HttpPropfind(URI.create(caldavUrl));
        request.setHeader("Depth", "0");

        try (CloseableHttpResponse response = client.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 207) {
                log.debug("Тест CalDAV подключения успешен (HTTP 207)");
                return;
            }

            String responseBody = EntityUtils.toString(response.getEntity());
            log.error("Тест CalDAV провален. Статус: {}", statusCode);

            throw new RuntimeException(
                    "Тест CalDAV провален. Статус: " + statusCode +
                            ", Ответ: " + responseBody
            );
        } catch (IOException e) {
            log.error("Ошибка CalDAV подключения: {}", e.getMessage());
            throw new RuntimeException("Ошибка CalDAV подключения: " + e.getMessage(), e);
        }
    }
}