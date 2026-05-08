package com.upsjb.ms1.util;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class DateTimeUtil {

    private DateTimeUtil() {
    }

    public static Instant now(Clock clock) {
        return Instant.now(clock);
    }

    public static Instant plusMinutes(Clock clock, long minutes) {
        return now(clock).plusSeconds(minutes * 60);
    }

    public static Instant plusDays(Clock clock, long days) {
        return now(clock).plusSeconds(days * 24 * 60 * 60);
    }

    public static LocalDateTime toLocalDateTime(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return null;
        }

        return LocalDateTime.ofInstant(instant, zoneId);
    }
}