package io.smallrye.faulttolerance.standalone.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.Guard;
import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.core.util.TestException;

public class StandaloneRetryTest {
    private int counter;

    @BeforeEach
    public void setUp() {
        counter = 0;
    }

    @Test
    public void retry() throws Exception {
        Callable<String> guarded = TypedGuard.create(String.class)
                .withRetry().maxRetries(3).done()
                .withFallback().applyOn(TestException.class).handler(this::fallback).done()
                .build()
                .adaptCallable(this::actionThrow);

        assertThat(guarded.call()).isEqualTo("fallback");
        assertThat(counter).isEqualTo(4); // 1 initial invocation + 3 retries
    }

    @Test
    public void retryWithAbortOn() throws Exception {
        Callable<String> guarded = TypedGuard.create(String.class)
                .withRetry().maxRetries(3).abortOn(TestException.class).done()
                .withFallback().applyOn(TestException.class).handler(this::fallback).done()
                .build()
                .adaptCallable(this::actionThrow);

        assertThat(guarded.call()).isEqualTo("fallback");
        assertThat(counter).isEqualTo(1); // 1 initial invocation
    }

    @Test
    public void retryWithRetryOn() throws Exception {
        Callable<String> guarded = TypedGuard.create(String.class)
                .withRetry().maxRetries(3).retryOn(RuntimeException.class).done()
                .withFallback().applyOn(TestException.class).handler(this::fallback).done()
                .build()
                .adaptCallable(this::actionThrow);

        assertThat(guarded.call()).isEqualTo("fallback");
        assertThat(counter).isEqualTo(1); // 1 initial invocation
    }

    @Test
    public void retryWithWhenException() throws Exception {
        Callable<String> guarded = TypedGuard.create(String.class)
                .withRetry().maxRetries(3).whenException(e -> e instanceof RuntimeException).done()
                .withFallback().when(e -> e instanceof TestException).handler(this::fallback).done()
                .build()
                .adaptCallable(this::actionThrow);

        assertThat(guarded.call()).isEqualTo("fallback");
        assertThat(counter).isEqualTo(1); // 1 initial invocation
    }

    @Test
    public void retryWithWhenResult() throws Exception {
        Callable<String> guarded = TypedGuard.create(String.class)
                .withRetry().maxRetries(3).whenResult(Objects::isNull).done()
                .withFallback().handler(this::fallback).done()
                .build()
                .adaptCallable(this::actionReturnNull);

        assertThat(guarded.call()).isEqualTo("fallback");
        assertThat(counter).isEqualTo(4); // 1 initial invocation + 3 retries
    }

    @Test
    public void retryWithComputedDelay() throws Exception {
        Callable<String> guarded = TypedGuard.create(String.class)
                .withRetry().maxRetries(3).computedDelay(e -> Duration.ofMillis(10)).done()
                .withFallback().applyOn(TestException.class).handler(this::fallback).done()
                .build()
                .adaptCallable(this::actionThrow);

        assertThat(guarded.call()).isEqualTo("fallback");
        assertThat(counter).isEqualTo(4); // 1 initial invocation + 3 retries
    }

    @Test
    public void retryWithComputedDelay_untyped() throws Exception {
        Callable<String> guarded = Guard.create()
                .withRetry().maxRetries(5).computedDelay(e -> Duration.ofMillis(10)).done()
                .build()
                .adaptCallable(() -> {
                    counter++;
                    if (counter < 4) {
                        throw new TestException();
                    }
                    return "success";
                }, String.class);

        assertThat(guarded.call()).isEqualTo("success");
        assertThat(counter).isEqualTo(4); // 1 initial invocation, 2 unsuccessful retries, 1 successful retry
    }

    public String actionThrow() throws TestException {
        counter++;
        throw new TestException();
    }

    public String actionReturnNull() throws TestException {
        counter++;
        return null;
    }

    public String fallback() {
        return "fallback";
    }
}
