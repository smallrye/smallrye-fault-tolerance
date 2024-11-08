package io.smallrye.faulttolerance.standalone.test;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.TypedGuard;

public class StandaloneTimeoutAsyncEventsTest {
    private volatile boolean shouldSleep;

    @Test
    public void asyncTimeoutEvents() throws Exception {
        AtomicInteger timeoutCounter = new AtomicInteger();
        AtomicInteger finishedCounter = new AtomicInteger();

        Callable<CompletionStage<String>> guarded = TypedGuard.create(Types.CS_STRING)
                .withTimeout()
                .duration(1, ChronoUnit.SECONDS)
                .onTimeout(timeoutCounter::incrementAndGet)
                .onFinished(finishedCounter::incrementAndGet)
                .done()
                .withFallback().applyOn(TimeoutException.class).handler(this::fallback).done()
                .withThreadOffload(true) // async timeout doesn't interrupt the running thread
                .build()
                .adaptCallable(this::action);

        shouldSleep = true;
        assertThat(guarded.call())
                .succeedsWithin(5, TimeUnit.SECONDS)
                .isEqualTo("fallback");
        assertThat(timeoutCounter).hasValue(1);
        assertThat(finishedCounter).hasValue(0);

        shouldSleep = false;
        assertThat(guarded.call())
                .succeedsWithin(5, TimeUnit.SECONDS)
                .isEqualTo("value");
        assertThat(timeoutCounter).hasValue(1);
        assertThat(finishedCounter).hasValue(1);
    }

    public CompletionStage<String> action() throws InterruptedException {
        if (shouldSleep) {
            Thread.sleep(10_000);
        }
        return completedFuture("value");
    }

    public CompletionStage<String> fallback() {
        return completedFuture("fallback");
    }
}
