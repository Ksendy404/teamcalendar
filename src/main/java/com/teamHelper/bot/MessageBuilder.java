package com.teamHelper.bot;

import com.teamHelper.model.CalendarEvent;
import org.springframework.stereotype.Component;

import static com.teamHelper.calendar.CalendarConstants.DATE_FORMAT;

@Component
public class MessageBuilder {

    public String buildEventMessage(CalendarEvent event) {
        return String.format(
                "🔔  %s\n" +
                "⏰  `%s` \\- `%s`\n\n" +
                        "%s" +
                        "%s",
                escapeMarkdownV2(event.getTitle()),
                escapeMarkdownV2(event.getStart().format(DATE_FORMAT)),
                escapeMarkdownV2(event.getEnd().format(DATE_FORMAT)),
                event.getDescription() != null ? "" + escapeMarkdownV2(event.getDescription()) + "\n" : "",
                event.getLocation() != null ? "*Место\\:* " + escapeMarkdownV2(event.getLocation().getTitle()) : ""
        );
    }

    public String escapeMarkdownV2(String text) {
        if (text == null) return "";
        return text.replaceAll("([_*\\[\\]()~`>#+=|{}\\.!\\-:])", "\\\\$1");
    }
}
