package io.smallrye.faulttolerance.config.better;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.RetryConfigBean/maxRetries\".retry.max-retries", value = "10")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.RetryConfigBean/maxDuration\".retry.max-duration", value = "1")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.RetryConfigBean/maxDuration\".retry.max-duration-unit", value = "seconds")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.RetryConfigBean/delay\".retry.delay", value = "2000")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.RetryConfigBean/delay\".retry.delay-unit", value = "micros")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.RetryConfigBean/retryOn\".retry.retry-on", value = "io.smallrye.faulttolerance.config.better.TestConfigExceptionA,io.smallrye.faulttolerance.config.better.TestConfigExceptionB")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.RetryConfigBean/abortOn\".retry.abort-on", value = "io.smallrye.faulttolerance.config.better.TestConfigExceptionA,io.smallrye.faulttolerance.config.better.TestConfigExceptionB1")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.RetryConfigBean/jitter\".retry.jitter", value = "1")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.RetryConfigBean/jitter\".retry.jitter-unit", value = "seconds")
public class RetryConfigTest {
    @Inject
    private RetryConfigBean bean;

    @Test
    public void maxRetries() {
        AtomicInteger counter = new AtomicInteger();
        assertThatThrownBy(() -> bean.maxRetries(counter)).isExactlyInstanceOf(TestException.class);
        assertThat(counter).hasValue(11);
    }

    @Test
    public void maxDuration() {
        long startTime = System.nanoTime();
        assertThatThrownBy(() -> bean.maxDuration()).isExactlyInstanceOf(TestException.class);
        long endTime = System.nanoTime();

        Duration duration = Duration.ofNanos(endTime - startTime);
        assertThat(duration).isLessThan(Duration.ofSeconds(8));
    }

    @Test
    public void delay() {
        long startTime = System.nanoTime();
        assertThatThrownBy(() -> bean.delay()).isExactlyInstanceOf(TestException.class);
        long endTime = System.nanoTime();

        Duration duration = Duration.ofNanos(endTime - startTime);
        assertThat(duration).isLessThan(Duration.ofSeconds(8));
    }

    @Test
    public void retryOn() {
        AtomicInteger counter = new AtomicInteger();

        counter.set(0);
        assertThatThrownBy(() -> bean.retryOn(new TestException(), counter)).isExactlyInstanceOf(TestException.class);
        assertThat(counter).hasValue(1);

        counter.set(0);
        assertThatThrownBy(() -> bean.retryOn(new TestConfigExceptionA(), counter))
                .isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThat(counter).hasValue(2);

        counter.set(0);
        assertThatThrownBy(() -> bean.retryOn(new TestConfigExceptionB(), counter))
                .isExactlyInstanceOf(TestConfigExceptionB.class);
        assertThat(counter).hasValue(2);

        counter.set(0);
        assertThatThrownBy(() -> bean.retryOn(new TestConfigExceptionB1(), counter))
                .isExactlyInstanceOf(TestConfigExceptionB1.class);
        assertThat(counter).hasValue(2);
    }

    @Test
    public void abortOn() {
        AtomicInteger counter = new AtomicInteger();

        counter.set(0);
        assertThatThrownBy(() -> bean.abortOn(new TestException(), counter)).isExactlyInstanceOf(TestException.class);
        assertThat(counter).hasValue(1);

        counter.set(0);
        assertThatThrownBy(() -> bean.abortOn(new TestConfigExceptionA(), counter))
                .isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThat(counter).hasValue(1);

        counter.set(0);
        assertThatThrownBy(() -> bean.abortOn(new TestConfigExceptionB(), counter))
                .isExactlyInstanceOf(TestConfigExceptionB.class);
        assertThat(counter).hasValue(2);

        counter.set(0);
        assertThatThrownBy(() -> bean.abortOn(new TestConfigExceptionB1(), counter))
                .isExactlyInstanceOf(TestConfigExceptionB1.class);
        assertThat(counter).hasValue(1);
    }

    @Test
    public void jitter() {
        assertThatThrownBy(() -> bean.jitter()).isExactlyInstanceOf(TestConfigExceptionA.class);
    }
}
