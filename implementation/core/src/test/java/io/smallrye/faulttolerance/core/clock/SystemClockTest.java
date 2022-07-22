package io.smallrye.faulttolerance.core.clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;

import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.LINUX)
public class SystemClockTest {
    private static final Percentage tolerance = withPercentage(25);

    private Clock clock = SystemClock.INSTANCE;

    @Test
    public void immediateMeasurement() {
        long time = clock.currentTimeInMillis();
        assertThat(time).isNotNegative();
        assertThat(clock.currentTimeInMillis() - time).isNotNegative();
    }

    @Test
    public void delayedMeasurement() throws InterruptedException {
        long time = clock.currentTimeInMillis();
        Thread.sleep(100);
        assertThat(clock.currentTimeInMillis() - time).isCloseTo(100, tolerance);
    }
}
