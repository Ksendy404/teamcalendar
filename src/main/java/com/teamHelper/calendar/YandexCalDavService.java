package com.teamHelper.calendar;

import com.teamHelper.config.HttpPropfind;
import com.teamHelper.config.HttpReport;
import com.teamHelper.model.CalendarEvent;
import jakarta.annotation.PreDestroy;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.component.VEvent;
import org.apache.commons.io.IOUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        this.client = HttpClients.custom()
                .setDefaultCredentialsProvider(new BasicCredentialsProvider() {{
                    setCredentials(AuthScope.ANY,
                            new UsernamePasswordCredentials(username, password));
                }})
                .build();
    }

    public List<CalendarEvent> getUpcomingEvents() throws Exception {
        try {
            LocalDateTime start = LocalDate.now().atTime(9, 0);
            LocalDateTime end = LocalDate.now().atTime(22, 0);   //TODO для отладки

            String calendarUrl = caldavUrl.endsWith("/") ? caldavUrl : caldavUrl + "/";

            // 2. Создаем REPORT запрос
            HttpReport request = new HttpReport(URI.create(calendarUrl));

            request.setHeader("Depth", "1");
            request.setHeader("Content-Type", "text/xml; charset=utf-8");
            request.setHeader("Prefer", "return-minimal");

            String xmlBody = calendarQueryBuilder.buildCalendarQuery(start, end);

            request.setEntity(new StringEntity(xmlBody, StandardCharsets.UTF_8));

            log.debug("Sending CalDAV REPORT request:\n{}", xmlBody);

            // 3. Выполняем запрос
            try (CloseableHttpResponse response = client.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                if (response.getStatusLine().getStatusCode() != 207) {
                    throw new RuntimeException(String.format(
                            "CalDAV error %d\nRequest:\n%s\nResponse:\n%s",
                            response.getStatusLine().getStatusCode(),
                            xmlBody,
                            responseBody
                    ));
                }
                return parseEvents(new ByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8)));
            }

        } catch (Exception e) {
            log.error("Failed to get upcoming events", e);
            throw new RuntimeException("Failed to fetch calendar events", e);
        }
    }

    private List<CalendarEvent> parseEvents(InputStream icalStream) throws Exception {
        // 1. Читаем ответ как строку
        String xmlResponse = IOUtils.toString(icalStream, StandardCharsets.UTF_8);
        log.debug("Raw XML response:\n{}", xmlResponse);

        // 2. Создаем XML парсер с поддержкой namespace
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        // 3. Парсим XML с обработкой ошибок
        Document doc;
        try {
            // Удаляем BOM если есть и другие невидимые символы
            String cleanXml = xmlResponse.replace("\uFEFF", "").trim();
            doc = builder.parse(new InputSource(new StringReader(cleanXml)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse XML: " + e.getMessage(), e);
        }

        // 4. Проверяем результат парсинга
        if (doc == null || doc.getDocumentElement() == null) {
            throw new RuntimeException("Invalid XML document structure");
        }
        log.debug("XML document parsed successfully");

        // 5. Ищем все ответы (response)
        NodeList responseNodes = doc.getElementsByTagNameNS("DAV:", "response");
        List<CalendarEvent> events = new ArrayList<>();
        CalendarBuilder calendarBuilder = new CalendarBuilder();

        // 6. Обрабатываем каждый response
        for (int i = 0; i < responseNodes.getLength(); i++) {
            Node responseNode = responseNodes.item(i);
            NodeList calendarDataNodes = ((Element) responseNode)
                    .getElementsByTagNameNS("urn:ietf:params:xml:ns:caldav", "calendar-data");

            for (int j = 0; j < calendarDataNodes.getLength(); j++) {
                String icalContent = calendarDataNodes.item(j).getTextContent();
                try {
                    // Чистим и нормализуем iCalendar данные
                    icalContent = icalContent.trim()
                            .replaceAll("\r", "")
                            .replaceAll("\n ", "\n");

                    if (icalContent.isEmpty()) continue;

                    log.debug("Processing iCalendar block #{}-{}:\n{}", i + 1, j + 1, icalContent);

                    // Парсим iCalendar
                    Calendar calendar = calendarBuilder.build(new StringReader(icalContent));
                    events.addAll(calendar.getComponents(Component.VEVENT).stream()
                            .map(c -> (VEvent) c)
                            .map(this::convertEvent)
                            .collect(Collectors.toList()));
                } catch (Exception e) {
                    log.error("Failed to parse iCalendar block #{}-{}: {}", i + 1, j + 1, e.getMessage());
                    log.debug("Problematic content:\n{}", icalContent);
                }
            }
        }

        log.info("Successfully parsed {} events", events.size());
        return events;
    }

    private CalendarEvent convertEvent(VEvent vEvent) {
        log.info("Converting event: {} (Start: {}, End: {})",
                vEvent.getSummary().getValue(),
                vEvent.getStartDate().getDate(),
                vEvent.getEndDate().getDate());

        CalendarEvent event = new CalendarEvent();

        // 1. Основные обязательные поля
        event.setId(vEvent.getUid().getValue());
        event.setTitle(vEvent.getSummary().getValue());

        // 2. Преобразование дат (с обработкой временных зон)
        if (vEvent.getStartDate() != null && vEvent.getStartDate().getDate() != null) {
            event.setStart(vEvent.getStartDate().getDate().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime());
        }

        if (vEvent.getEndDate() != null && vEvent.getEndDate().getDate() != null) {
            event.setEnd(vEvent.getEndDate().getDate().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime());
        }

        // 3. Опциональные поля (с проверкой на null)
        if (vEvent.getDescription() != null) {
            event.setDescription(vEvent.getDescription().getValue());
        }

        if (vEvent.getUrl() != null) {
            event.setUrl(vEvent.getUrl().getValue());
        }

        // 4. Локация (если есть)
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
        log.info("Testing CalDAV connection to {}", caldavUrl);

        // 1. Создаем простой PROPFIND запрос (стандартный для CalDAV)
        HttpPropfind request = new HttpPropfind(URI.create(caldavUrl));
        request.setHeader("Depth", "0"); // Только для указанного URL

        // 2. Отправляем запрос
        try (CloseableHttpResponse response = client.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();

            // 3. Проверяем код ответа
            if (statusCode == 207) { // 207 Multi-Status - ожидаемый ответ для PROPFIND
                log.debug("CalDAV connection test successful (HTTP 207)");
                return;
            }

            // 4. Обрабатываем ошибки
            String responseBody = EntityUtils.toString(response.getEntity());
            throw new RuntimeException(
                    "CalDAV test failed. Status: " + statusCode +
                            ", Response: " + responseBody
            );
        } catch (IOException e) {
            throw new RuntimeException("CalDAV connection error: " + e.getMessage(), e);
        }
    }
}