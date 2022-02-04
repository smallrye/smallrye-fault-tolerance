package io.smallrye.faulttolerance.standalone.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.core.util.TestException;

public class StandaloneRetryEventsTest {
    private int failTimes;

    @Test
    public void retryEvents() throws Exception {
        AtomicInteger retryCounter = new AtomicInteger();
        AtomicInteger successCounter = new AtomicInteger();
        AtomicInteger failureCounter = new AtomicInteger();

        Callable<String> guarded = FaultTolerance.createCallable(this::action)
                .withRetry()
                .maxRetries(3)
                .onRetry(retryCounter::incrementAndGet)
                .onSuccess(successCounter::incrementAndGet)
                .onFailure(failureCounter::incrementAndGet)
                .done()
                .withFallback().applyOn(TestException.class).handler(this::fallback).done()
                .build();

        failTimes = 10;
        assertThat(guarded.call()).isEqualTo("fallback");
        assertThat(retryCounter).hasValue(3);
        assertThat(successCounter).hasValue(0);
        assertThat(failureCounter).hasValue(1);

        failTimes = 2;
        assertThat(guarded.call()).isEqualTo("value");
        assertThat(retryCounter).hasValue(5);
        assertThat(successCounter).hasValue(1);
        assertThat(failureCounter).hasValue(1);
    }

    public String action() throws TestException {
        if (failTimes > 0) {
            failTimes--;
            throw new TestException();
        }

        return "value";
    }

    public String fallback() {
        return "fallback";
    }
}
