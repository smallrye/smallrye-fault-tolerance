package io.smallrye.faulttolerance.core.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class ConstantBackOffTest {
    @Test
    public void positiveDelay_zeroJitter() {
        BackOff backOff = new ConstantBackOff(100, Jitter.ZERO);

        for (int i = 0; i < 100; i++) {
            assertThat(backOff.getInMillis(null)).isEqualTo(100);
        }
    }

    @Test
    public void positiveDelay_fixedJitter() {
        BackOff backOff = new ConstantBackOff(100, new FixedJitter(50));

        for (int i = 0; i < 100; i++) {
            assertThat(backOff.getInMillis(null)).isEqualTo(150);
        }
    }

    @Test
    public void positiveDelay_randomJitter() {
        BackOff backOff = new ConstantBackOff(100, new RandomJitter(50));

        for (int i = 0; i < 100; i++) {
            assertThat(backOff.getInMillis(null)).isGreaterThanOrEqualTo(50).isLessThanOrEqualTo(150);
        }

    }

    @Test
    public void zeroDelay_zeroJitter() {
        BackOff backOff = new ConstantBackOff(0, Jitter.ZERO);

        for (int i = 0; i < 100; i++) {
            assertThat(backOff.getInMillis(null)).isEqualTo(0);
        }
    }

    @Test
    public void zeroDelay_fixedJitter() {
        BackOff backOff = new ConstantBackOff(0, new FixedJitter(50));

        for (int i = 0; i < 100; i++) {
            assertThat(backOff.getInMillis(null)).isEqualTo(50);
        }
    }

    @Test
    public void zeroDelay_randomJitter() {
        BackOff backOff = new ConstantBackOff(0, new RandomJitter(50));

        for (int i = 0; i < 100; i++) {
            assertThat(backOff.getInMillis(null)).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(50);
        }
    }

    @Test
    public void negativeDelay() {
        assertThatThrownBy(() -> new ConstantBackOff(-1, Jitter.ZERO))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void nullJitter() {
        assertThatThrownBy(() -> new ConstantBackOff(1, null))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }
}
