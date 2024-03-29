package io.smallrye.faulttolerance.core.stopwatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;

import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.LINUX)
public class SystemStopwatchTest {
    private static final Percentage tolerance = withPercentage(25);

    private Stopwatch stopwatch = SystemStopwatch.INSTANCE;

    @Test
    public void immediateMeasurement() {
        RunningStopwatch runningStopwatch = stopwatch.start();
        assertThat(runningStopwatch.elapsedTimeInMillis()).isCloseTo(0, tolerance);
    }

    @Test
    public void delayedMeasurement() throws InterruptedException {
        RunningStopwatch runningStopwatch = stopwatch.start();
        Thread.sleep(100);
        assertThat(runningStopwatch.elapsedTimeInMillis()).isCloseTo(100, tolerance);
    }
}
