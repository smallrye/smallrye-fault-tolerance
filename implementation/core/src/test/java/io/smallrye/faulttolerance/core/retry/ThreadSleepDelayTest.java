package io.smallrye.faulttolerance.core.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Percentage.withPercentage;

import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;

public class ThreadSleepDelayTest {
    private static final Percentage tolerance = withPercentage(10);

    @Test
    public void positiveDelay_zeroJitter() throws InterruptedException {
        Delay delay = new ThreadSleepDelay(100, Jitter.ZERO);

        long start = System.nanoTime();
        delay.sleep();
        long timeInMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(timeInMillis).isCloseTo(100, tolerance);
    }

    @Test
    public void positiveDelay_fixedJitter() throws InterruptedException {
        Delay delay = new ThreadSleepDelay(100, new FixedJitter(50));

        long start = System.nanoTime();
        delay.sleep();
        long timeInMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(timeInMillis).isCloseTo(150, tolerance);
    }

    @Test
    public void positiveDelay_randomJitter() throws InterruptedException {
        Delay delay = new ThreadSleepDelay(100, new RandomJitter(50));

        long start = System.nanoTime();
        delay.sleep();
        long timeInMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(timeInMillis).isGreaterThanOrEqualTo(50).isLessThanOrEqualTo(150 + /* tolerance */ 10);
    }

    @Test
    public void zeroDelay_zeroJitter() throws InterruptedException {
        Delay delay = new ThreadSleepDelay(0, Jitter.ZERO);

        long start = System.nanoTime();
        delay.sleep();
        long timeInMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(timeInMillis).isCloseTo(0, tolerance);
    }

    @Test
    public void zeroDelay_fixedJitter() throws InterruptedException {
        Delay delay = new ThreadSleepDelay(0, new FixedJitter(50));

        long start = System.nanoTime();
        delay.sleep();
        long timeInMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(timeInMillis).isCloseTo(50, tolerance);
    }

    @Test
    public void zeroDelay_randomJitter() throws InterruptedException {
        Delay delay = new ThreadSleepDelay(0, new RandomJitter(50));

        long start = System.nanoTime();
        delay.sleep();
        long timeInMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(timeInMillis).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(50 + /* tolerance */ 10);
    }

    @Test
    public void negativeDelay() {
        assertThatThrownBy(() -> new ThreadSleepDelay(-1, Jitter.ZERO)).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void nullJitter() {
        assertThatThrownBy(() -> new ThreadSleepDelay(1, null)).isExactlyInstanceOf(IllegalArgumentException.class);
    }
}
