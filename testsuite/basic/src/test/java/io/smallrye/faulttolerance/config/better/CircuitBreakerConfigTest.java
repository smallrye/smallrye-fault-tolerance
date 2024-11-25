package io.smallrye.faulttolerance.config.better;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.CircuitBreakerConfigBean/skipOn\".circuit-breaker.skip-on", value = "io.smallrye.faulttolerance.config.better.TestConfigExceptionA")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.CircuitBreakerConfigBean/failOn\".circuit-breaker.fail-on", value = "io.smallrye.faulttolerance.config.better.TestConfigExceptionA")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.CircuitBreakerConfigBean/delay\".circuit-breaker.delay", value = "1000")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.CircuitBreakerConfigBean/delay\".circuit-breaker.delay-unit", value = "millis")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.CircuitBreakerConfigBean/requestVolumeThreshold\".circuit-breaker.request-volume-threshold", value = "4")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.CircuitBreakerConfigBean/failureRatio\".circuit-breaker.failure-ratio", value = "0.8")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.CircuitBreakerConfigBean/successThreshold\".circuit-breaker.success-threshold", value = "2")
public class CircuitBreakerConfigTest {
    @Inject
    private CircuitBreakerConfigBean bean;

    @Test
    public void failOn() {
        assertThatThrownBy(() -> bean.failOn()).isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThatThrownBy(() -> bean.failOn()).isExactlyInstanceOf(TestConfigExceptionA.class);

        assertThatThrownBy(() -> bean.failOn()).isExactlyInstanceOf(CircuitBreakerOpenException.class);
    }

    @Test
    public void testConfigureSkipOn() {
        assertThatThrownBy(() -> bean.skipOn()).isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThatThrownBy(() -> bean.skipOn()).isExactlyInstanceOf(TestConfigExceptionA.class);

        assertThatThrownBy(() -> bean.skipOn()).isExactlyInstanceOf(TestConfigExceptionA.class);
    }

    @Test
    public void delay() {
        assertThatThrownBy(() -> bean.delay(true)).isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThatThrownBy(() -> bean.delay(true)).isExactlyInstanceOf(TestConfigExceptionA.class);

        assertThatThrownBy(() -> bean.delay(false)).isExactlyInstanceOf(CircuitBreakerOpenException.class);

        long start = System.nanoTime();
        await().atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
            assertThatCode(() -> bean.delay(false)).doesNotThrowAnyException();
        });
        long end = System.nanoTime();

        long durationInMillis = Duration.ofNanos(end - start).toMillis();
        assertThat(durationInMillis).isGreaterThan(800);
        assertThat(durationInMillis).isLessThan(2000);
    }

    @Test
    public void requestVolumeThreshold() {
        assertThatThrownBy(() -> bean.requestVolumeThreshold()).isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThatThrownBy(() -> bean.requestVolumeThreshold()).isExactlyInstanceOf(TestConfigExceptionA.class);

        assertThatThrownBy(() -> bean.requestVolumeThreshold()).isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThatThrownBy(() -> bean.requestVolumeThreshold()).isExactlyInstanceOf(TestConfigExceptionA.class);

        assertThatThrownBy(() -> bean.requestVolumeThreshold()).isExactlyInstanceOf(CircuitBreakerOpenException.class);
    }

    @Test
    public void failureRatio() {
        assertThatThrownBy(() -> bean.failureRatio(true)).isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThatThrownBy(() -> bean.failureRatio(true)).isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThatCode(() -> bean.failureRatio(false)).doesNotThrowAnyException();
        assertThatThrownBy(() -> bean.failureRatio(true)).isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThatThrownBy(() -> bean.failureRatio(true)).isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThatThrownBy(() -> bean.failureRatio(true)).isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThatCode(() -> bean.failureRatio(false)).doesNotThrowAnyException();
        assertThatThrownBy(() -> bean.failureRatio(true)).isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThatThrownBy(() -> bean.failureRatio(true)).isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThatThrownBy(() -> bean.failureRatio(true)).isExactlyInstanceOf(TestConfigExceptionA.class);

        assertThatThrownBy(() -> bean.failureRatio(false)).isExactlyInstanceOf(CircuitBreakerOpenException.class);
    }

    @Test
    public void successThreshold() {
        for (int i = 0; i < 10; i++) {
            assertThatThrownBy(() -> bean.successThreshold(true)).isExactlyInstanceOf(TestConfigExceptionA.class);
        }

        assertThatThrownBy(() -> bean.successThreshold(false)).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        await().atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
            assertThatCode(() -> bean.successThreshold(false)).doesNotThrowAnyException();
        });

        assertThatCode(() -> bean.successThreshold(false)).doesNotThrowAnyException();

        for (int i = 0; i < 10; i++) {
            assertThatThrownBy(() -> bean.successThreshold(true)).isExactlyInstanceOf(TestConfigExceptionA.class);
        }

        assertThatThrownBy(() -> bean.successThreshold(false)).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        await().atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
            assertThatCode(() -> bean.successThreshold(false)).doesNotThrowAnyException();
        });

        assertThatThrownBy(() -> bean.successThreshold(true)).isExactlyInstanceOf(TestConfigExceptionA.class);

        assertThatThrownBy(() -> bean.successThreshold(false)).isExactlyInstanceOf(CircuitBreakerOpenException.class);
    }
}
