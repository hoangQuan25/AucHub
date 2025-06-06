package com.example.deliveries.utils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class DateTimeUtil {
    public static LocalDateTime roundToMicrosecond(LocalDateTime ldt) {
        if (ldt == null) {
            return null;
        }
        // Get the nanoseconds part
        int nano = ldt.getNano();
        // Round the nanoseconds to the nearest multiple of 1000 (for microseconds)
        // For example, 123456 ns -> 123000 ns; 123789 ns -> 124000 ns
        int roundedMicrosecondsAsNanos = (int) (Math.round(nano / 1000.0) * 1000.0);

        // Handle case where rounding nanoseconds results in a new second
        if (roundedMicrosecondsAsNanos == 1_000_000_000) {
            return ldt.truncatedTo(ChronoUnit.SECONDS).plusSeconds(1);
        } else {
            return ldt.truncatedTo(ChronoUnit.SECONDS).withNano(roundedMicrosecondsAsNanos);
        }
    }
}
