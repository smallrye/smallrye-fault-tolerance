package io.smallrye.faulttolerance.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

public class DurationsTest {
    @Test
    public void timeInMillis() {
        assertThat(Durations.timeInMillis(5_000_000, ChronoUnit.NANOS)).isEqualTo(5);
        assertThat(Durations.timeInMillis(5_000, ChronoUnit.MICROS)).isEqualTo(5);
        assertThat(Durations.timeInMillis(5, ChronoUnit.MILLIS)).isEqualTo(5);
        assertThat(Durations.timeInMillis(2, ChronoUnit.SECONDS)).isEqualTo(2000);
        assertThat(Durations.timeInMillis(2, ChronoUnit.MINUTES)).isEqualTo(120_000);
        assertThat(Durations.timeInMillis(3, ChronoUnit.HOURS)).isEqualTo(Duration.ofHours(3).toMillis());
        assertThat(Durations.timeInMillis(2, ChronoUnit.HALF_DAYS)).isEqualTo(Duration.ofDays(1).toMillis());
        assertThat(Durations.timeInMillis(8, ChronoUnit.HALF_DAYS)).isEqualTo(Duration.ofDays(4).toMillis());
        assertThat(Durations.timeInMillis(365, ChronoUnit.DAYS)).isEqualTo(Duration.ofDays(365).toMillis());
        assertThat(Durations.timeInMillis(7, ChronoUnit.WEEKS)).isEqualTo(Duration.ofDays(7 * 7).toMillis());
        assertThat(Durations.timeInMillis(17, ChronoUnit.WEEKS)).isEqualTo(Duration.ofDays(17 * 7).toMillis());
        assertThat(Durations.timeInMillis(12, ChronoUnit.MONTHS)).isEqualTo(ChronoUnit.YEARS.getDuration().toMillis());
        assertThat(Durations.timeInMillis(24, ChronoUnit.MONTHS)).isEqualTo(2 * ChronoUnit.YEARS.getDuration().toMillis());

        assertThat(Durations.timeInMillis(0, ChronoUnit.HALF_DAYS)).isEqualTo(0);
        assertThat(Durations.timeInMillis(4, ChronoUnit.HALF_DAYS)).isEqualTo(Duration.ofDays(2).toMillis());
        assertThat(Durations.timeInMillis(-4, ChronoUnit.HALF_DAYS)).isEqualTo(Duration.ofDays(-2).toMillis());
        assertThat(Durations.timeInMillis(0, ChronoUnit.DAYS)).isEqualTo(0);
        assertThat(Durations.timeInMillis(4, ChronoUnit.DAYS)).isEqualTo(Duration.ofDays(4).toMillis());
        assertThat(Durations.timeInMillis(-4, ChronoUnit.DAYS)).isEqualTo(Duration.ofDays(-4).toMillis());
        assertThat(Durations.timeInMillis(0, ChronoUnit.WEEKS)).isEqualTo(0);
        assertThat(Durations.timeInMillis(4, ChronoUnit.WEEKS)).isEqualTo(Duration.ofDays(4 * 7).toMillis());
        assertThat(Durations.timeInMillis(-4, ChronoUnit.WEEKS)).isEqualTo(Duration.ofDays(-4 * 7).toMillis());
        assertThat(Durations.timeInMillis(0, ChronoUnit.MONTHS)).isEqualTo(0);
        assertThat(Durations.timeInMillis(7, ChronoUnit.MONTHS)).isEqualTo(7 * ChronoUnit.MONTHS.getDuration().toMillis());
        assertThat(Durations.timeInMillis(-7, ChronoUnit.MONTHS)).isEqualTo(-7 * ChronoUnit.MONTHS.getDuration().toMillis());

        assertThat(Durations.timeInMillis(Long.MAX_VALUE, ChronoUnit.HOURS)).isEqualTo(Long.MAX_VALUE);
        assertThat(Durations.timeInMillis(Long.MAX_VALUE, ChronoUnit.HALF_DAYS)).isEqualTo(Long.MAX_VALUE);
        assertThat(Durations.timeInMillis(Long.MAX_VALUE, ChronoUnit.DAYS)).isEqualTo(Long.MAX_VALUE);
        assertThat(Durations.timeInMillis(Long.MAX_VALUE, ChronoUnit.WEEKS)).isEqualTo(Long.MAX_VALUE);
        assertThat(Durations.timeInMillis(Long.MAX_VALUE, ChronoUnit.MONTHS)).isEqualTo(Long.MAX_VALUE);

        assertThat(Durations.timeInMillis(Long.MAX_VALUE - 1, ChronoUnit.HOURS)).isEqualTo(Long.MAX_VALUE);
        assertThat(Durations.timeInMillis(Long.MAX_VALUE - 1, ChronoUnit.HALF_DAYS)).isEqualTo(Long.MAX_VALUE);
        assertThat(Durations.timeInMillis(Long.MAX_VALUE - 1, ChronoUnit.DAYS)).isEqualTo(Long.MAX_VALUE);
        assertThat(Durations.timeInMillis(Long.MAX_VALUE - 1, ChronoUnit.WEEKS)).isEqualTo(Long.MAX_VALUE);
        assertThat(Durations.timeInMillis(Long.MAX_VALUE - 1, ChronoUnit.MONTHS)).isEqualTo(Long.MAX_VALUE);

        assertThat(Durations.timeInMillis(Long.MIN_VALUE + 1, ChronoUnit.HOURS)).isEqualTo(Long.MIN_VALUE);
        assertThat(Durations.timeInMillis(Long.MIN_VALUE + 1, ChronoUnit.HALF_DAYS)).isEqualTo(Long.MIN_VALUE);
        assertThat(Durations.timeInMillis(Long.MIN_VALUE + 1, ChronoUnit.DAYS)).isEqualTo(Long.MIN_VALUE);
        assertThat(Durations.timeInMillis(Long.MIN_VALUE + 1, ChronoUnit.WEEKS)).isEqualTo(Long.MIN_VALUE);
        assertThat(Durations.timeInMillis(Long.MIN_VALUE + 1, ChronoUnit.MONTHS)).isEqualTo(Long.MIN_VALUE);

        assertThat(Durations.timeInMillis(Long.MIN_VALUE, ChronoUnit.HOURS)).isEqualTo(Long.MIN_VALUE);
        assertThat(Durations.timeInMillis(Long.MIN_VALUE, ChronoUnit.HALF_DAYS)).isEqualTo(Long.MIN_VALUE);
        assertThat(Durations.timeInMillis(Long.MIN_VALUE, ChronoUnit.DAYS)).isEqualTo(Long.MIN_VALUE);
        assertThat(Durations.timeInMillis(Long.MIN_VALUE, ChronoUnit.WEEKS)).isEqualTo(Long.MIN_VALUE);
        assertThat(Durations.timeInMillis(Long.MIN_VALUE, ChronoUnit.MONTHS)).isEqualTo(Long.MIN_VALUE);
    }

    @Test
    public void timeInMillis_random() {
        for (ChronoUnit unit : List.of(ChronoUnit.NANOS, ChronoUnit.MICROS, ChronoUnit.MILLIS, ChronoUnit.SECONDS,
                ChronoUnit.MINUTES, ChronoUnit.HOURS, ChronoUnit.HALF_DAYS, ChronoUnit.DAYS)) {
            for (int i = 0; i < 1_000_000; i++) {
                long value = ThreadLocalRandom.current().nextLong(Integer.MIN_VALUE * 1000L, Integer.MAX_VALUE * 1000L);
                assertThat(Durations.timeInMillis(value, unit))
                        .isEqualTo(TimeUnit.MILLISECONDS.convert(Duration.of(value, unit)));
            }
        }
    }

    @Test
    public void timeInNanos() {
        assertThat(Durations.timeInNanos(5, ChronoUnit.NANOS)).isEqualTo(5);
        assertThat(Durations.timeInNanos(5, ChronoUnit.MICROS)).isEqualTo(5_000);
        assertThat(Durations.timeInNanos(5, ChronoUnit.MILLIS)).isEqualTo(5_000_000);
        assertThat(Durations.timeInNanos(2, ChronoUnit.SECONDS)).isEqualTo(2_000_000_000);
        assertThat(Durations.timeInNanos(2, ChronoUnit.MINUTES)).isEqualTo(120_000_000_000L);
        assertThat(Durations.timeInNanos(3, ChronoUnit.HOURS)).isEqualTo(Duration.ofHours(3).toNanos());
        assertThat(Durations.timeInNanos(2, ChronoUnit.HALF_DAYS)).isEqualTo(Duration.ofDays(1).toNanos());
        assertThat(Durations.timeInNanos(8, ChronoUnit.HALF_DAYS)).isEqualTo(Duration.ofDays(4).toNanos());
        assertThat(Durations.timeInNanos(365, ChronoUnit.DAYS)).isEqualTo(Duration.ofDays(365).toNanos());
        assertThat(Durations.timeInNanos(7, ChronoUnit.WEEKS)).isEqualTo(Duration.ofDays(7 * 7).toNanos());
        assertThat(Durations.timeInNanos(12, ChronoUnit.MONTHS)).isEqualTo(ChronoUnit.YEARS.getDuration().toNanos());

        assertThat(Durations.timeInNanos(0, ChronoUnit.DAYS)).isEqualTo(0);
        assertThat(Durations.timeInNanos(-4, ChronoUnit.DAYS)).isEqualTo(Duration.ofDays(-4).toNanos());

        assertThat(Durations.timeInNanos(Long.MAX_VALUE, ChronoUnit.NANOS)).isEqualTo(Long.MAX_VALUE);
        assertThat(Durations.timeInNanos(Long.MAX_VALUE, ChronoUnit.MICROS)).isEqualTo(Long.MAX_VALUE);
        assertThat(Durations.timeInNanos(Long.MAX_VALUE, ChronoUnit.MILLIS)).isEqualTo(Long.MAX_VALUE);
        assertThat(Durations.timeInNanos(Long.MAX_VALUE, ChronoUnit.SECONDS)).isEqualTo(Long.MAX_VALUE);
        assertThat(Durations.timeInNanos(Long.MAX_VALUE, ChronoUnit.MINUTES)).isEqualTo(Long.MAX_VALUE);
        assertThat(Durations.timeInNanos(Long.MAX_VALUE, ChronoUnit.HOURS)).isEqualTo(Long.MAX_VALUE);
        assertThat(Durations.timeInNanos(Long.MAX_VALUE, ChronoUnit.HALF_DAYS)).isEqualTo(Long.MAX_VALUE);
        assertThat(Durations.timeInNanos(Long.MAX_VALUE, ChronoUnit.DAYS)).isEqualTo(Long.MAX_VALUE);
        assertThat(Durations.timeInNanos(Long.MAX_VALUE, ChronoUnit.WEEKS)).isEqualTo(Long.MAX_VALUE);
        assertThat(Durations.timeInNanos(Long.MAX_VALUE, ChronoUnit.MONTHS)).isEqualTo(Long.MAX_VALUE);

        assertThat(Durations.timeInNanos(Long.MIN_VALUE, ChronoUnit.NANOS)).isEqualTo(Long.MIN_VALUE);
        assertThat(Durations.timeInNanos(Long.MIN_VALUE, ChronoUnit.MICROS)).isEqualTo(Long.MIN_VALUE);
        assertThat(Durations.timeInNanos(Long.MIN_VALUE, ChronoUnit.MILLIS)).isEqualTo(Long.MIN_VALUE);
        assertThat(Durations.timeInNanos(Long.MIN_VALUE, ChronoUnit.SECONDS)).isEqualTo(Long.MIN_VALUE);
        assertThat(Durations.timeInNanos(Long.MIN_VALUE, ChronoUnit.MINUTES)).isEqualTo(Long.MIN_VALUE);
        assertThat(Durations.timeInNanos(Long.MIN_VALUE, ChronoUnit.HOURS)).isEqualTo(Long.MIN_VALUE);
        assertThat(Durations.timeInNanos(Long.MIN_VALUE, ChronoUnit.HALF_DAYS)).isEqualTo(Long.MIN_VALUE);
        assertThat(Durations.timeInNanos(Long.MIN_VALUE, ChronoUnit.DAYS)).isEqualTo(Long.MIN_VALUE);
        assertThat(Durations.timeInNanos(Long.MIN_VALUE, ChronoUnit.WEEKS)).isEqualTo(Long.MIN_VALUE);
        assertThat(Durations.timeInNanos(Long.MIN_VALUE, ChronoUnit.MONTHS)).isEqualTo(Long.MIN_VALUE);
    }

    @Test
    public void timeInNanos_random() {
        for (ChronoUnit unit : List.of(ChronoUnit.NANOS, ChronoUnit.MICROS, ChronoUnit.MILLIS, ChronoUnit.SECONDS,
                ChronoUnit.MINUTES, ChronoUnit.HOURS, ChronoUnit.HALF_DAYS, ChronoUnit.DAYS)) {
            for (int i = 0; i < 1_000_000; i++) {
                long value = ThreadLocalRandom.current().nextLong(Integer.MIN_VALUE * 1000L, Integer.MAX_VALUE * 1000L);
                assertThat(Durations.timeInNanos(value, unit))
                        .isEqualTo(TimeUnit.NANOSECONDS.convert(Duration.of(value, unit)));
            }
        }
    }
}
