package io.smallrye.faulttolerance.standalone.test;

import static io.smallrye.faulttolerance.core.util.CompletionStages.completedStage;
import static io.smallrye.faulttolerance.core.util.CompletionStages.failedStage;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.core.util.TestException;

public class StandaloneRetryAsyncEventsTest {
    private int failTimes;

    @Test
    public void asyncRetry() {
        AtomicInteger retryCounter = new AtomicInteger();
        AtomicInteger successCounter = new AtomicInteger();
        AtomicInteger failureCounter = new AtomicInteger();

        Supplier<CompletionStage<String>> guarded = FaultTolerance.createAsyncSupplier(this::action)
                .withRetry()
                .maxRetries(3)
                .onRetry(retryCounter::incrementAndGet)
                .onSuccess(successCounter::incrementAndGet)
                .onFailure(failureCounter::incrementAndGet)
                .done()
                .withFallback().handler(this::fallback).applyOn(TestException.class).done()
                .build();

        failTimes = 10;
        assertThat(guarded.get())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");
        assertThat(retryCounter).hasValue(3);
        assertThat(successCounter).hasValue(0);
        assertThat(failureCounter).hasValue(1);

        failTimes = 2;
        assertThat(guarded.get())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("value");
        assertThat(retryCounter).hasValue(5);
        assertThat(successCounter).hasValue(1);
        assertThat(failureCounter).hasValue(1);
    }

    public CompletionStage<String> action() {
        if (failTimes > 0) {
            failTimes--;
            return failedStage(new TestException());
        }

        return completedStage("value");
    }

    public CompletionStage<String> fallback() {
        return completedStage("fallback");
    }
}
