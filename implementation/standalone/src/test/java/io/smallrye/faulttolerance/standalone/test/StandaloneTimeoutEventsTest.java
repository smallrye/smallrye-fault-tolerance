package io.smallrye.faulttolerance.standalone.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.TypedGuard;

public class StandaloneTimeoutEventsTest {
    private boolean shouldSleep;

    @Test
    public void timeoutEvents() throws Exception {
        AtomicInteger timeoutCounter = new AtomicInteger();
        AtomicInteger finishedCounter = new AtomicInteger();

        Callable<String> guarded = TypedGuard.create(String.class)
                .withTimeout()
                .duration(1000, ChronoUnit.MILLIS)
                .onTimeout(timeoutCounter::incrementAndGet)
                .onFinished(finishedCounter::incrementAndGet)
                .done()
                .withFallback().applyOn(TimeoutException.class).handler(this::fallback).done()
                .build()
                .adaptCallable(this::action);

        shouldSleep = true;
        assertThat(guarded.call()).isEqualTo("fallback");
        assertThat(timeoutCounter).hasValue(1);
        assertThat(finishedCounter).hasValue(0);

        shouldSleep = false;
        assertThat(guarded.call()).isEqualTo("value");
        assertThat(timeoutCounter).hasValue(1);
        assertThat(finishedCounter).hasValue(1);
    }

    public String action() throws InterruptedException {
        if (shouldSleep) {
            Thread.sleep(10_000);
        }
        return "value";
    }

    public String fallback() {
        return "fallback";
    }
}
