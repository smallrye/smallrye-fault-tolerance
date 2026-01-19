package io.smallrye.faulttolerance.core.util;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public final class Durations {
    private static final long SECONDS_IN_HALF_DAY = ChronoUnit.HALF_DAYS.getDuration().getSeconds();
    private static final long SECONDS_IN_WEEK = ChronoUnit.WEEKS.getDuration().getSeconds();;
    private static final long SECONDS_IN_MONTH = ChronoUnit.MONTHS.getDuration().getSeconds();;
    private static final long SECONDS_IN_YEAR = ChronoUnit.YEARS.getDuration().getSeconds();;

    private static final long MAX_HALF_DAYS = Long.MAX_VALUE / SECONDS_IN_HALF_DAY;
    private static final long MAX_WEEKS = Long.MAX_VALUE / SECONDS_IN_WEEK;
    private static final long MAX_MONTHS = Long.MAX_VALUE / SECONDS_IN_MONTH;
    private static final long MAX_YEARS = Long.MAX_VALUE / SECONDS_IN_YEAR;

    public static long timeInMillis(long value, ChronoUnit unit) {
        return switch (unit) {
            case NANOS -> TimeUnit.NANOSECONDS.toMillis(value);
            case MICROS -> TimeUnit.MICROSECONDS.toMillis(value);
            case MILLIS -> value;
            case SECONDS -> TimeUnit.SECONDS.toMillis(value);
            case MINUTES -> TimeUnit.MINUTES.toMillis(value);
            case HOURS -> TimeUnit.HOURS.toMillis(value);
            case HALF_DAYS -> convert(value, MAX_HALF_DAYS, SECONDS_IN_HALF_DAY);
            case DAYS -> TimeUnit.DAYS.toMillis(value);
            case WEEKS -> convert(value, MAX_WEEKS, SECONDS_IN_WEEK);
            case MONTHS -> convert(value, MAX_MONTHS, SECONDS_IN_MONTH);
            case YEARS -> convert(value, MAX_YEARS, SECONDS_IN_YEAR);
            default -> throw new IllegalArgumentException("Unsupported time unit: " + unit);
        };
    }

    private static long convert(long value, long maxInUnit, long secondsInUnit) {
        if (value == Long.MIN_VALUE) {
            return Long.MIN_VALUE;
        }

        boolean negative = value < 0;
        long abs = negative ? -value : value;
        if (abs > maxInUnit) {
            // `value * secondsInUnit` would overflow
            return negative ? Long.MIN_VALUE : Long.MAX_VALUE;
        }

        return TimeUnit.SECONDS.toMillis(value * secondsInUnit);
    }
}
