package io.smallrye.faulttolerance.core.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.SplittableRandom;

import org.junit.Test;

public class RandomJitterTest {
    @Test
    public void negative() {
        SplittableRandom rng = new SplittableRandom();
        for (int i = 0; i < 10_000; i++) {
            long random = rng.nextLong(Long.MIN_VALUE, 0);
            assertThatThrownBy(() -> new RandomJitter(random)).isExactlyInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    public void zero() {
        assertThatThrownBy(() -> new RandomJitter(0)).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void positive() {
        SplittableRandom rng = new SplittableRandom();
        for (int i = 0; i < 100; i++) {
            long random = rng.nextLong(0, Long.MAX_VALUE);

            Jitter jitter = new RandomJitter(random);
            for (int j = 0; j < 10_000; j++) {
                assertThat(jitter.generate())
                        .isGreaterThanOrEqualTo(-1 * random)
                        .isLessThanOrEqualTo(random);
            }
        }
    }
}
