package io.smallrye.faulttolerance.config.better;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.TimeoutConfigBean/value\".timeout.value", value = "1000")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.TimeoutConfigBean/unit\".timeout.unit", value = "millis")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.TimeoutConfigBean/both\".timeout.value", value = "1000")
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.config.better.TimeoutConfigBean/both\".timeout.unit", value = "millis")
public class TimeoutConfigTest {
    @Inject
    private TimeoutConfigBean bean;

    @Test
    public void value() {
        doTest(() -> bean.value());
    }

    @Test
    public void unit() {
        doTest(() -> bean.unit());
    }

    @Test
    public void both() {
        doTest(() -> {
            try {
                bean.both().toCompletableFuture().get(1, TimeUnit.MINUTES);
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        });
    }

    private void doTest(ThrowingCallable action) {
        long start = System.nanoTime();
        assertThatThrownBy(action).isExactlyInstanceOf(TimeoutException.class);
        long end = System.nanoTime();

        long durationInMillis = Duration.ofNanos(end - start).toMillis();
        assertThat(durationInMillis).isGreaterThan(800);
        assertThat(durationInMillis).isLessThan(2000);
    }
}
