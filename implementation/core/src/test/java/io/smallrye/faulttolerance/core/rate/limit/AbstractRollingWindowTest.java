package io.smallrye.faulttolerance.core.rate.limit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;
import io.smallrye.faulttolerance.core.stopwatch.TestStopwatch;

public abstract class AbstractRollingWindowTest {
    private TestStopwatch stopwatch;

    @BeforeEach
    public void setUp() {
        stopwatch = new TestStopwatch();
    }

    @Test
    public void scenario1() {
        TimeWindow window = createRollingWindow(stopwatch, 2, 100, 0);

        // 0
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(100);

        stopwatch.setCurrentValue(50);

        // 50
        assertThat(window.record()).isEqualTo(50);

        stopwatch.setCurrentValue(100);

        // 100
        assertThat(window.record()).isEqualTo(0);
    }

    @Test
    public void scenario2() {
        TimeWindow window = createRollingWindow(stopwatch, 2, 100, 0);

        // 0
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(100);

        stopwatch.setCurrentValue(50);

        // 50
        assertThat(window.record()).isEqualTo(50);
        assertThat(window.record()).isEqualTo(50);

        stopwatch.setCurrentValue(120);

        // 120
        assertThat(window.record()).isEqualTo(30);

        stopwatch.setCurrentValue(190);

        // 190
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(30);
    }

    @Test
    public void scenario3() {
        TimeWindow window = createRollingWindow(stopwatch, 4, 100, 0);

        // 0
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(100);

        stopwatch.setCurrentValue(50);

        // 50
        assertThat(window.record()).isEqualTo(50);

        stopwatch.setCurrentValue(100);

        // 100
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(50);

        stopwatch.setCurrentValue(200);

        // 200
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(100);
    }

    @Test
    public void scenario4() {
        TimeWindow window = createRollingWindow(stopwatch, 4, 100, 0);

        // 0
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(100);

        stopwatch.setCurrentValue(50);

        // 50
        assertThat(window.record()).isEqualTo(50);

        stopwatch.setCurrentValue(75);

        // 75
        assertThat(window.record()).isEqualTo(25);

        stopwatch.setCurrentValue(100);

        // 100
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(50);

        stopwatch.setCurrentValue(200);

        // 200
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(100);
    }

    @Test
    public void scenario5() {
        TimeWindow window = createRollingWindow(stopwatch, 4, 100, 5);

        // 0
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(5);

        stopwatch.setCurrentValue(10);

        // 10
        assertThat(window.record()).isEqualTo(0);

        stopwatch.setCurrentValue(13);

        // 13
        assertThat(window.record()).isEqualTo(2);

        stopwatch.setCurrentValue(50);

        // 50
        assertThat(window.record()).isEqualTo(50);

        stopwatch.setCurrentValue(100);

        // 100
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(10);

        stopwatch.setCurrentValue(120);

        // 120
        assertThat(window.record()).isEqualTo(0);

        stopwatch.setCurrentValue(130);

        // 130
        assertThat(window.record()).isEqualTo(20);
    }

    @Test
    public void scenario6() {
        TimeWindow window = createRollingWindow(stopwatch, 2, 100, 0);

        // 0
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(100);

        stopwatch.setCurrentValue(550);

        // 550
        assertThat(window.record()).isEqualTo(0);

        stopwatch.setCurrentValue(1050);

        // 1050
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(0);
        assertThat(window.record()).isEqualTo(100);
    }

    @Test
    public void scenario7() {
        TimeWindow window = createRollingWindow(stopwatch, 4, 100, 5);

        // 0
        assertThat(window.record()).isEqualTo(0);

        stopwatch.setCurrentValue(25);

        // 25
        assertThat(window.record()).isEqualTo(0);

        stopwatch.setCurrentValue(28);

        // 28
        assertThat(window.record()).isEqualTo(2);

        stopwatch.setCurrentValue(50);

        // 50
        assertThat(window.record()).isEqualTo(0);

        stopwatch.setCurrentValue(100);

        // 100
        assertThat(window.record()).isEqualTo(0);

        stopwatch.setCurrentValue(123);

        // 123
        assertThat(window.record()).isEqualTo(2);

        stopwatch.setCurrentValue(130);

        // 130
        assertThat(window.record()).isEqualTo(0);
    }

    protected abstract TimeWindow createRollingWindow(Stopwatch stopwatch, int maxInvocations, long timeWindowInMillis,
            long minSpacingInMillis);
}
