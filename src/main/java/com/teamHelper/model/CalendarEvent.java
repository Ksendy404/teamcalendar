package com.teamHelper.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CalendarEvent {
    private String id;
    private String title;
    private String description;
    private String url;
    private LocalDateTime start;
    private LocalDateTime end;
    private Location location;
    private List<Attendee> attendees;

    @Data
    public static class Location {
        private String title;
        private String url;
    }

    @Data
    public static class Attendee {
        private String name;
        private String email;
    }
}