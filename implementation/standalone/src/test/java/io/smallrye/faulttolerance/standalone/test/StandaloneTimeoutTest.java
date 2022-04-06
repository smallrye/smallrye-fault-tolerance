package io.smallrye.faulttolerance.standalone.test;

import static io.smallrye.faulttolerance.core.util.Timing.timed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.FaultTolerance;

public class StandaloneTimeoutTest {
    @Test
    public void timeout() throws Exception {
        Callable<String> guarded = FaultTolerance.createCallable(this::action)
                .withTimeout().duration(1000, ChronoUnit.MILLIS).done()
                .withFallback().applyOn(TimeoutException.class).handler(this::fallback).done()
                .build();

        long time = timed(() -> {
            assertThat(guarded.call()).isEqualTo("fallback");
        });
        assertThat(time).isCloseTo(1000, withinPercentage(50));
    }

    public String action() throws InterruptedException {
        Thread.sleep(10_000);
        return "value";
    }

    public String fallback() {
        return "fallback";
    }
}
