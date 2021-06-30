package io.smallrye.faulttolerance.core.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class FibonacciBackOffTest {
    @Test
    public void fibonacciSequence() {
        BackOff backOff = new FibonacciBackOff(1, Jitter.ZERO, Long.MAX_VALUE);

        assertThat(backOff.getInMillis(null)).isEqualTo(1);
        assertThat(backOff.getInMillis(null)).isEqualTo(2);
        assertThat(backOff.getInMillis(null)).isEqualTo(3);
        assertThat(backOff.getInMillis(null)).isEqualTo(5);
        assertThat(backOff.getInMillis(null)).isEqualTo(8);
        assertThat(backOff.getInMillis(null)).isEqualTo(13);
        assertThat(backOff.getInMillis(null)).isEqualTo(21);
        assertThat(backOff.getInMillis(null)).isEqualTo(34);
        assertThat(backOff.getInMillis(null)).isEqualTo(55);
        assertThat(backOff.getInMillis(null)).isEqualTo(89);
        assertThat(backOff.getInMillis(null)).isEqualTo(144);
    }

    @Test
    public void fibonacciSequence_fixedJitter() {
        BackOff backOff = new FibonacciBackOff(1, new FixedJitter(1_000_000), Long.MAX_VALUE);

        assertThat(backOff.getInMillis(null)).isEqualTo(1_000_001);
        assertThat(backOff.getInMillis(null)).isEqualTo(1_000_002);
        assertThat(backOff.getInMillis(null)).isEqualTo(1_000_003);
        assertThat(backOff.getInMillis(null)).isEqualTo(1_000_005);
        assertThat(backOff.getInMillis(null)).isEqualTo(1_000_008);
        assertThat(backOff.getInMillis(null)).isEqualTo(1_000_013);
        assertThat(backOff.getInMillis(null)).isEqualTo(1_000_021);
        assertThat(backOff.getInMillis(null)).isEqualTo(1_000_034);
        assertThat(backOff.getInMillis(null)).isEqualTo(1_000_055);
        assertThat(backOff.getInMillis(null)).isEqualTo(1_000_089);
        assertThat(backOff.getInMillis(null)).isEqualTo(1_000_144);
    }

    @Test
    public void negativeInitialDelay() {
        assertThatThrownBy(() -> new FibonacciBackOff(-1, Jitter.ZERO, 2))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void nullJitter() {
        assertThatThrownBy(() -> new FibonacciBackOff(1, null, 2))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void initialDelayEqualToMaxDelay() {
        assertThatThrownBy(() -> new FibonacciBackOff(2, Jitter.ZERO, 2))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void initialDelayGreaterThanMaxDelay() {
        assertThatThrownBy(() -> new FibonacciBackOff(3, Jitter.ZERO, 2))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }
}
