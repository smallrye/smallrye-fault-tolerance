package io.smallrye.faulttolerance.standalone.test;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.core.util.TestException;

public class StandaloneRetryAsyncTest {
    private final AtomicInteger counter = new AtomicInteger();

    @BeforeEach
    public void setUp() {
        counter.set(0);
    }

    @Test
    public void asyncRetry() {
        Supplier<CompletionStage<String>> guarded = TypedGuard.create(Types.CS_STRING)
                .withRetry().maxRetries(3).done()
                .withFallback().handler(this::fallback).applyOn(TestException.class).done()
                .build()
                .adaptSupplier(this::actionFail);

        assertThat(guarded.get())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");
        assertThat(counter).hasValue(4); // 1 initial invocation + 3 retries
    }

    @Test
    public void asyncRetryWithAbortOn() {
        Supplier<CompletionStage<String>> guarded = TypedGuard.create(Types.CS_STRING)
                .withRetry().maxRetries(3).abortOn(TestException.class).done()
                .withFallback().handler(this::fallback).applyOn(TestException.class).done()
                .build()
                .adaptSupplier(this::actionFail);

        assertThat(guarded.get())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");
        assertThat(counter).hasValue(1); // 1 initial invocation
    }

    @Test
    public void asyncRetryWithRetryOn() {
        Supplier<CompletionStage<String>> guarded = TypedGuard.create(Types.CS_STRING)
                .withRetry().maxRetries(3).retryOn(RuntimeException.class).done()
                .withFallback().handler(this::fallback).applyOn(TestException.class).done()
                .build()
                .adaptSupplier(this::actionFail);

        assertThat(guarded.get())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");
        assertThat(counter).hasValue(1); // 1 initial invocation
    }

    @Test
    public void asyncRetryWithWhenException() {
        Supplier<CompletionStage<String>> guarded = TypedGuard.create(Types.CS_STRING)
                .withRetry().maxRetries(3).whenException(e -> e instanceof RuntimeException).done()
                .withFallback().handler(this::fallback).when(e -> e instanceof TestException).done()
                .build()
                .adaptSupplier(this::actionFail);

        assertThat(guarded.get())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");
        assertThat(counter).hasValue(1); // 1 initial invocation
    }

    @Test
    public void asyncRetryWithWhenResult() {
        Supplier<CompletionStage<String>> guarded = TypedGuard.create(Types.CS_STRING)
                .withRetry().maxRetries(3).whenResult(Objects::isNull).done()
                .withFallback().handler(this::fallback).done()
                .build()
                .adaptSupplier(this::actionReturnNull);

        assertThat(guarded.get())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");
        assertThat(counter).hasValue(4); // 1 initial invocation + 3 retries
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
