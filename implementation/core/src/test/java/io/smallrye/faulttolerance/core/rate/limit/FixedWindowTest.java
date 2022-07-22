package io.smallrye.faulttolerance.core.rate.limit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.clock.TestClock;

public class FixedWindowTest {
    private TestClock clock;

    @BeforeEach
    public void setUp() {
        clock = new TestClock();
    }

    @Test
    public void scenario1() {
        TimeWindow window = new FixedWindow(clock, 2, 100, 0);

        // 0
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isFalse();

        clock.step(50);

        // 50
        assertThat(window.record()).isFalse();

        clock.step(50);

        // 100
        assertThat(window.record()).isTrue();
    }

    @Test
    public void scenario2() {
        TimeWindow window = new FixedWindow(clock, 2, 100, 0);

        // 0
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isFalse();

        clock.step(50);

        // 50
        assertThat(window.record()).isFalse();
        assertThat(window.record()).isFalse();

        clock.step(70);

        // 120
        assertThat(window.record()).isTrue();

        clock.step(70);

        // 190
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isFalse();

        clock.step(20);

        // 210
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isFalse();

        clock.step(80);

        // 290
        assertThat(window.record()).isFalse();
    }

    @Test
    public void scenario3() {
        TimeWindow window = new FixedWindow(clock, 4, 100, 0);

        // 0
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isFalse();

        clock.step(50);

        // 50
        assertThat(window.record()).isFalse();

        clock.step(50);

        // 100
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isFalse();

        clock.step(100);

        // 200
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isFalse();
    }

    @Test
    public void scenario4() {
        TimeWindow window = new FixedWindow(clock, 4, 100, 0);

        // 0
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isFalse();

        clock.step(50);

        // 50
        assertThat(window.record()).isFalse();

        clock.step(25);

        // 75
        assertThat(window.record()).isFalse();

        clock.step(25);

        // 100
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isFalse();

        clock.step(100);

        // 200
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isFalse();
    }

    @Test
    public void scenario5() {
        TimeWindow window = new FixedWindow(clock, 4, 100, 5);

        // 0
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isFalse();

        clock.step(10);

        // 10
        assertThat(window.record()).isTrue();

        clock.step(3);

        // 13
        assertThat(window.record()).isFalse();

        clock.step(37);

        // 50
        assertThat(window.record()).isFalse();

        clock.step(50);

        // 100
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isFalse();

        clock.step(20);

        // 120
        assertThat(window.record()).isTrue();

        clock.step(10);

        // 130
        assertThat(window.record()).isTrue();
    }

    @Test
    public void scenario6() {
        TimeWindow window = new FixedWindow(clock, 2, 100, 0);

        // 0
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isFalse();

        clock.step(550);

        // 550
        assertThat(window.record()).isTrue();

        clock.step(500);

        // 1050
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isTrue();
        assertThat(window.record()).isFalse();
    }
}
