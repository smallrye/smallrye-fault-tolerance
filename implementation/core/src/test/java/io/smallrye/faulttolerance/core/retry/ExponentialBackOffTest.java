package io.smallrye.faulttolerance.core.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class ExponentialBackOffTest {
    @Test
    public void exponentialSequence() {
        BackOff backOff = new ExponentialBackOff(1, 2, Jitter.ZERO, Long.MAX_VALUE);

        assertThat(backOff.getInMillis(null)).isEqualTo(1);
        assertThat(backOff.getInMillis(null)).isEqualTo(2);
        assertThat(backOff.getInMillis(null)).isEqualTo(4);
        assertThat(backOff.getInMillis(null)).isEqualTo(8);
        assertThat(backOff.getInMillis(null)).isEqualTo(16);
        assertThat(backOff.getInMillis(null)).isEqualTo(32);
        assertThat(backOff.getInMillis(null)).isEqualTo(64);
        assertThat(backOff.getInMillis(null)).isEqualTo(128);
        assertThat(backOff.getInMillis(null)).isEqualTo(256);
        assertThat(backOff.getInMillis(null)).isEqualTo(512);
        assertThat(backOff.getInMillis(null)).isEqualTo(1024);
    }

    @Test
    public void exponentialSequence_fixedJitter() {
        BackOff backOff = new ExponentialBackOff(1, 2, new FixedJitter(1_000_000), Long.MAX_VALUE);

        assertThat(backOff.getInMillis(null)).isEqualTo(1_000_001);
        assertThat(backOff.getInMillis(null)).isEqualTo(1_000_002);
        assertThat(backOff.getInMillis(null)).isEqualTo(1_000_004);
        assertThat(backOff.getInMillis(null)).isEqualTo(1_000_008);
        assertThat(backOff.getInMillis(null)).isEqualTo(1_000_016);
        assertThat(backOff.getInMillis(null)).isEqualTo(1_000_032);
        assertThat(backOff.getInMillis(null)).isEqualTo(1_000_064);
        assertThat(backOff.getInMillis(null)).isEqualTo(1_000_128);
        assertThat(backOff.getInMillis(null)).isEqualTo(1_000_256);
        assertThat(backOff.getInMillis(null)).isEqualTo(1_000_512);
        assertThat(backOff.getInMillis(null)).isEqualTo(1_00_1024);
    }

    @Test
    public void negativeInitialDelay() {
        assertThatThrownBy(() -> new ExponentialBackOff(-1, 1, Jitter.ZERO, 2))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void negativeFactor() {
        assertThatThrownBy(() -> new ExponentialBackOff(1, -1, Jitter.ZERO, 2))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void zeroFactor() {
        assertThatThrownBy(() -> new ExponentialBackOff(1, 0, Jitter.ZERO, 2))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void nullJitter() {
        assertThatThrownBy(() -> new ExponentialBackOff(1, 1, null, 2))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void initialDelayEqualToMaxDelay() {
        assertThatThrownBy(() -> new ExponentialBackOff(2, 1, Jitter.ZERO, 2))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void initialDelayGreaterThanMaxDelay() {
        assertThatThrownBy(() -> new ExponentialBackOff(3, 1, Jitter.ZERO, 2))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }
}
