package io.smallrye.faulttolerance.standalone.test;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.core.util.TestException;

public class StandaloneRetryAsyncTest {
    private final AtomicInteger counter = new AtomicInteger();

    @BeforeEach
    public void setUp() {
        counter.set(0);
    }

    @Test
    public void asyncRetry() {
        Supplier<CompletionStage<String>> guarded = FaultTolerance.createAsyncSupplier(this::actionFail)
                .withRetry().maxRetries(3).done()
                .withFallback().handler(this::fallback).applyOn(TestException.class).done()
                .build();

        assertThat(guarded.get())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");
        assertThat(counter).hasValue(4); // 1 initial invocation + 3 retries
    }

    @Test
    public void asyncRetryWithAbortOn() {
        Supplier<CompletionStage<String>> guarded = FaultTolerance.createAsyncSupplier(this::actionFail)
                .withRetry().maxRetries(3).abortOn(TestException.class).done()
                .withFallback().handler(this::fallback).applyOn(TestException.class).done()
                .build();

        assertThat(guarded.get())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");
        assertThat(counter).hasValue(1); // 1 initial invocation
    }

    @Test
    public void asyncRetryWithRetryOn() {
        Supplier<CompletionStage<String>> guarded = FaultTolerance.createAsyncSupplier(this::actionFail)
                .withRetry().maxRetries(3).retryOn(RuntimeException.class).done()
                .withFallback().handler(this::fallback).applyOn(TestException.class).done()
                .build();

        assertThat(guarded.get())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");
        assertThat(counter).hasValue(1); // 1 initial invocation
    }

    @Test
    public void asyncRetryWithWhenException() {
        Supplier<CompletionStage<String>> guarded = FaultTolerance.createAsyncSupplier(this::actionFail)
                .withRetry().maxRetries(3).whenException(e -> e instanceof RuntimeException).done()
                .withFallback().handler(this::fallback).when(e -> e instanceof TestException).done()
                .build();

        assertThat(guarded.get())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");
        assertThat(counter).hasValue(1); // 1 initial invocation
    }

    @Test
    public void asyncRetryWithWhenResult() {
        Supplier<CompletionStage<String>> guarded = FaultTolerance.createAsyncSupplier(this::actionReturnNull)
                .withRetry().maxRetries(3).whenResult(Objects::isNull).done()
                .withFallback().handler(this::fallback).done()
                .build();

        assertThat(guarded.get())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");
        assertThat(counter).hasValue(4); // 1 initial invocation + 3 retries
    }

    @Test
    public void synchronousFlow() {
        // this is usually a mistake, because it only guards the synchronous execution
        // only testing it here to verify that indeed asynchronous fault tolerance doesn't apply
        Supplier<CompletionStage<String>> guarded = FaultTolerance.createSupplier(this::actionFail)
                .withRetry().maxRetries(3).abortOn(TestException.class).done()
                .withFallback().handler(this::fallback).done()
                .build();

        assertThat(guarded.get())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
        assertThat(counter).hasValue(1); // 1 initial invocation
    }

    public CompletionStage<String> actionFail() {
        counter.incrementAndGet();
        return failedFuture(new TestException());
    }

    public CompletionStage<String> actionReturnNull() {
        counter.incrementAndGet();
        return completedFuture(null);
    }

    public CompletionStage<String> fallback() {
        return completedFuture("fallback");
    }
}
