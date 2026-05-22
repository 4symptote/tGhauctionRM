package com.app.client.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtil {
    private static final SimpleDateFormat EXACT_FORMAT = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");

    public static String formatExactDate(long timestamp) {
        return EXACT_FORMAT.format(new Date(timestamp));
    }

    public static String formatCountdown(long targetTimeMillis) {
        long timeLeft = targetTimeMillis - System.currentTimeMillis();

        if (timeLeft <= 0) return "Ended";

        long days = timeLeft / (1000 * 60 * 60 * 24);
        long hours = (timeLeft / (1000 * 60 * 60)) % 24;
        long minutes = (timeLeft / (1000 * 60)) % 60;
        long seconds = (timeLeft / 1000) % 60;

        if (days > 0) {
            return String.format("%dd %02d:%02d:%02d", days, hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
    }
}