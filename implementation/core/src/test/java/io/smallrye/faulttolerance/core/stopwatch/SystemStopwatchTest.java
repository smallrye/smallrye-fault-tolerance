package io.smallrye.faulttolerance.core.stopwatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;

import org.assertj.core.data.Percentage;
import org.junit.Test;

public class SystemStopwatchTest {
    private static final Percentage tolerance = withPercentage(10);

    private Stopwatch stopwatch = new SystemStopwatch();

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
