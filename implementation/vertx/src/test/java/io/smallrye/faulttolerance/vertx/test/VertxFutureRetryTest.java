package io.smallrye.faulttolerance.vertx.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.core.util.TestException;
import io.vertx.core.Future;

public class VertxFutureRetryTest {
    private final AtomicInteger counter = new AtomicInteger();

    @BeforeEach
    public void setUp() {
        counter.set(0);
    }

    @Test
    public void retry() {
        Supplier<Future<String>> guarded = TypedGuard.create(Types.FUTURE_STRING)
                .withRetry().maxRetries(3).done()
                .withFallback().handler(this::fallback).applyOn(TestException.class).done()
                .build()
                .adaptSupplier(this::action);

        assertThat(guarded.get().toCompletionStage())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");
        assertThat(counter).hasValue(4); // 1 initial invocation + 3 retries
    }

    @Test
    public void retryWithAbortOn() {
        Supplier<Future<String>> guarded = TypedGuard.create(Types.FUTURE_STRING)
                .withRetry().maxRetries(3).abortOn(TestException.class).done()
                .withFallback().handler(this::fallback).applyOn(TestException.class).done()
                .build()
                .adaptSupplier(this::action);

        assertThat(guarded.get().toCompletionStage())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");
        assertThat(counter).hasValue(1); // 1 initial invocation
    }

    @Test
    public void retryWithRetryOn() {
        Supplier<Future<String>> guarded = TypedGuard.create(Types.FUTURE_STRING)
                .withRetry().maxRetries(3).retryOn(RuntimeException.class).done()
                .withFallback().handler(this::fallback).applyOn(TestException.class).done()
                .build()
                .adaptSupplier(this::action);

        assertThat(guarded.get().toCompletionStage())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");
        assertThat(counter).hasValue(1); // 1 initial invocation
    }

    @Test
    public void retryWithWhen() {
        Supplier<Future<String>> guarded = TypedGuard.create(Types.FUTURE_STRING)
                .withRetry().maxRetries(3).whenException(e -> e instanceof RuntimeException).done()
                .withFallback().handler(this::fallback).when(e -> e instanceof TestException).done()
                .build()
                .adaptSupplier(this::action);

        assertThat(guarded.get().toCompletionStage())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");
        assertThat(counter).hasValue(1); // 1 initial invocation
    }

    public Future<String> action() {
        counter.incrementAndGet();
        return Future.failedFuture(new TestException());
    }

    public Future<String> fallback() {
        return Future.succeededFuture("fallback");
    }
}
