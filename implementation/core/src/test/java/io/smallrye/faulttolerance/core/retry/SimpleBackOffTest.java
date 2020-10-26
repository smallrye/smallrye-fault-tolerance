package io.smallrye.faulttolerance.core.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class SimpleBackOffTest {
    @Test
    public void positiveDelay_zeroJitter() {
        BackOff backOff = new SimpleBackOff(100, Jitter.ZERO);

        for (int i = 0; i < 100; i++) {
            assertThat(backOff.getInMillis()).isEqualTo(100);
        }
    }

    @Test
    public void positiveDelay_fixedJitter() {
        BackOff backOff = new SimpleBackOff(100, new FixedJitter(50));

        for (int i = 0; i < 100; i++) {
            assertThat(backOff.getInMillis()).isEqualTo(150);
        }
    }

    @Test
    public void positiveDelay_randomJitter() {
        BackOff backOff = new SimpleBackOff(100, new RandomJitter(50));

        for (int i = 0; i < 100; i++) {
            assertThat(backOff.getInMillis()).isGreaterThanOrEqualTo(50).isLessThanOrEqualTo(150);
        }

    }

    @Test
    public void zeroDelay_zeroJitter() {
        BackOff backOff = new SimpleBackOff(0, Jitter.ZERO);

        for (int i = 0; i < 100; i++) {
            assertThat(backOff.getInMillis()).isEqualTo(0);
        }
    }

    @Test
    public void zeroDelay_fixedJitter() {
        BackOff backOff = new SimpleBackOff(0, new FixedJitter(50));

        for (int i = 0; i < 100; i++) {
            assertThat(backOff.getInMillis()).isEqualTo(50);
        }
    }

    @Test
    public void zeroDelay_randomJitter() {
        BackOff backOff = new SimpleBackOff(0, new RandomJitter(50));

        for (int i = 0; i < 100; i++) {
            assertThat(backOff.getInMillis()).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(50);
        }
    }

    @Test
    public void negativeDelay() {
        assertThatThrownBy(() -> new SimpleBackOff(-1, Jitter.ZERO))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void nullJitter() {
        assertThatThrownBy(() -> new SimpleBackOff(1, null))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }
}
