package com.example.todolist.utils;
import java.util.Locale;

public class TimePickerHelper {

    public static String formatTimeForDisplay(String time) {
        try {
            String[] parts = time.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            return formatTimeForDisplay(hour, minute);
        } catch (Exception e) {
            return time;
        }
    }

    public static String formatTimeForDisplay(int hour, int minute) {
        String period = "AM";
        int displayHour = hour;

        if (hour >= 12) {
            period = "PM";
            if (hour > 12) {
                displayHour = hour - 12;
            }
        }
        if (hour == 0) {
            displayHour = 12;
        }

        return String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, period);
    }
}