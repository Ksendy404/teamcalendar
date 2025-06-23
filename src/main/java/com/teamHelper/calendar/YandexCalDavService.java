package com.teamHelper.calendar;

import com.teamHelper.config.HttpPropfind;
import com.teamHelper.config.HttpReport;
import com.teamHelper.model.CalendarEvent;
import jakarta.annotation.PreDestroy;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.PeriodList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
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
import java.util.Date;
import java.util.List;

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

    public List<CalendarEvent> getUpcomingEvents() throws Exception {
        try {
            LocalDateTime start = LocalDate.now().atTime(9, 0);
            LocalDateTime end = LocalDate.now().atTime(18, 0);

            String calendarUrl = caldavUrl.endsWith("/") ? caldavUrl : caldavUrl + "/";

            HttpReport request = new HttpReport(URI.create(calendarUrl));
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

                // Логируем только если есть события и уровень DEBUG
                if (log.isDebugEnabled() && !events.isEmpty()) {
                    log.debug("Получено {} событий из календаря", events.size());
                }

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
        net.fortuna.ical4j.model.DateTime icalStart = new net.fortuna.ical4j.model.DateTime(
                Date.from(periodStart.atZone(zoneId).toInstant()));
        net.fortuna.ical4j.model.DateTime icalEnd = new net.fortuna.ical4j.model.DateTime(
                Date.from(periodEnd.atZone(zoneId).toInstant()));

        net.fortuna.ical4j.model.Period period = new net.fortuna.ical4j.model.Period(icalStart, icalEnd);

        for (Object component : calendar.getComponents(Component.VEVENT)) {
            VEvent vEvent = (VEvent) component;

            try {
                // Проверяем, есть ли правило повторения
                if (vEvent.getProperty(Property.RRULE) != null) {
                    // Повторяющееся событие - логируем только в DEBUG
                    if (log.isDebugEnabled()) {
                        log.debug("Разворачиваем повторяющееся событие: '{}'",
                                vEvent.getSummary() != null ? vEvent.getSummary().getValue() : "Без названия");
                    }

                    // Получаем все экземпляры события в указанном периоде
                    PeriodList periodList = vEvent.calculateRecurrenceSet(period);

                    for (Object periodObj : periodList) {
                        net.fortuna.ical4j.model.Period eventPeriod = (net.fortuna.ical4j.model.Period) periodObj;

                        // Создаем копию события для каждого повторения
                        CalendarEvent expandedEvent = convertEventWithCustomTime(
                                vEvent,
                                eventPeriod.getStart(),
                                eventPeriod.getEnd());
                        expandedEvents.add(expandedEvent);
                    }
                } else {
                    // Обычное событие без повторений
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

    private CalendarEvent convertEventWithCustomTime(VEvent vEvent, net.fortuna.ical4j.model.DateTime startTime,
                                                     net.fortuna.ical4j.model.DateTime endTime) {
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