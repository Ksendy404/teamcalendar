package com.teamHelper.calendar;

import java.time.format.DateTimeFormatter;

public class CalendarConstants {

    public static final int CHECK_INTERVAL_MINUTES = 1;  // Интервал проверки (в минутах)
    public static final int NOTIFY_BEFORE_MINUTES = 5;   // Уведомлять за N минут до события
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
}
