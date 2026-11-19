package io.smallrye.faulttolerance.core.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

public class ComputedBackOffTest {
    @Test
    public void positiveDelay_zeroJitter() {
        BackOff fallback = new ConstantBackOff(100, Jitter.ZERO);
        BackOff backOff = new ComputedBackOff((result, cause) -> Duration.ofMillis(500), fallback, Jitter.ZERO);

        assertThat(backOff.getInMillis(null, new RuntimeException())).isEqualTo(500);
    }

    @Test
    public void positiveDelay_fixedJitter() {
        BackOff fallback = new ConstantBackOff(100, Jitter.ZERO);
        BackOff backOff = new ComputedBackOff((result, cause) -> Duration.ofMillis(500), fallback, new FixedJitter(50));

        assertThat(backOff.getInMillis(null, new RuntimeException())).isEqualTo(550);
    }

    @Test
    public void positiveDelay_randomJitter() {
        BackOff fallback = new ConstantBackOff(100, Jitter.ZERO);
        BackOff backOff = new ComputedBackOff((result, cause) -> Duration.ofMillis(500), fallback, new RandomJitter(50));

        for (int i = 0; i < 100; i++) {
            assertThat(backOff.getInMillis(null, new RuntimeException())).isGreaterThanOrEqualTo(450).isLessThanOrEqualTo(550);
        }
    }

    @Test
    public void zeroDelay_zeroJitter() {
        BackOff fallback = new ConstantBackOff(100, Jitter.ZERO);
        BackOff backOff = new ComputedBackOff((result, cause) -> Duration.ZERO, fallback, Jitter.ZERO);

        assertThat(backOff.getInMillis(null, new RuntimeException())).isEqualTo(0);
    }

    @Test
    public void zeroDelay_fixedJitter() {
        BackOff fallback = new ConstantBackOff(100, Jitter.ZERO);
        BackOff backOff = new ComputedBackOff((result, cause) -> Duration.ZERO, fallback, new FixedJitter(50));

        assertThat(backOff.getInMillis(null, new RuntimeException())).isEqualTo(50);
    }

    @Test
    public void zeroDelay_randomJitter() {
        BackOff fallback = new ConstantBackOff(100, Jitter.ZERO);
        BackOff backOff = new ComputedBackOff((result, cause) -> Duration.ZERO, fallback, new RandomJitter(50));

        for (int i = 0; i < 100; i++) {
            assertThat(backOff.getInMillis(null, new RuntimeException())).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(50);
        }
    }

    @Test
    public void nullDelayFallsBack() {
        BackOff fallback = new ConstantBackOff(100, Jitter.ZERO);
        BackOff backOff = new ComputedBackOff((cause, result) -> null, fallback, Jitter.ZERO);

        assertThat(backOff.getInMillis(null, new RuntimeException())).isEqualTo(100);
    }

    @Test
    public void negativeDelayFallsBack() {
        BackOff fallback = new ConstantBackOff(100, Jitter.ZERO);
        BackOff backOff = new ComputedBackOff((cause, result) -> Duration.ofMillis(-50), fallback, Jitter.ZERO);

        assertThat(backOff.getInMillis(null, new RuntimeException())).isEqualTo(100);
    }

    @Test
    public void exceptionFallsBack() {
        BackOff fallback = new ConstantBackOff(100, Jitter.ZERO);
        BackOff backOff = new ComputedBackOff((cause, result) -> {
            throw new RuntimeException("error extracting delay");
        }, fallback, Jitter.ZERO);

        assertThat(backOff.getInMillis(null, new RuntimeException())).isEqualTo(100);
    }

    @Test
    public void nullArguments() {
        BackOff fallback = new ConstantBackOff(100, Jitter.ZERO);
        assertThatThrownBy(() -> new ComputedBackOff(null, fallback, Jitter.ZERO))
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ComputedBackOff((cause, result) -> Duration.ofMillis(100), null, Jitter.ZERO))
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ComputedBackOff((cause, result) -> Duration.ofMillis(100), fallback, null))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }
}
