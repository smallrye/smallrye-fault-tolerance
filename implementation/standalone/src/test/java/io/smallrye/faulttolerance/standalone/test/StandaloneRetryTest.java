package io.smallrye.faulttolerance.standalone.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.core.util.TestException;

public class StandaloneRetryTest {
    private int counter;

    @BeforeEach
    public void setUp() {
        counter = 0;
    }

    @Test
    public void retry() throws Exception {
        Callable<String> guarded = FaultTolerance.createCallable(this::action)
                .withRetry().maxRetries(3).done()
                .withFallback().applyOn(TestException.class).handler(this::fallback).done()
                .build();

        assertThat(guarded.call()).isEqualTo("fallback");
        assertThat(counter).isEqualTo(4); // 1 initial invocation + 3 retries
    }

    @Test
    public void retryWithAbortOn() throws Exception {
        Callable<String> guarded = FaultTolerance.createCallable(this::action)
                .withRetry().maxRetries(3).abortOn(TestException.class).done()
                .withFallback().applyOn(TestException.class).handler(this::fallback).done()
                .build();

        assertThat(guarded.call()).isEqualTo("fallback");
        assertThat(counter).isEqualTo(1); // 1 initial invocation
    }

    @Test
    public void retryWithRetryOn() throws Exception {
        Callable<String> guarded = FaultTolerance.createCallable(this::action)
                .withRetry().maxRetries(3).retryOn(RuntimeException.class).done()
                .withFallback().applyOn(TestException.class).handler(this::fallback).done()
                .build();

        assertThat(guarded.call()).isEqualTo("fallback");
        assertThat(counter).isEqualTo(1); // 1 initial invocation
    }

    public String action() throws TestException {
        counter++;
        throw new TestException();
    }

    public String fallback() {
        return "fallback";
    }
}
