package io.smallrye.faulttolerance.standalone.test;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.Guard;
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

    @Test
    public void asyncRetryWithComputedDelay() {
        Supplier<CompletionStage<String>> guarded = TypedGuard.create(Types.CS_STRING)
                .withRetry().maxRetries(3).computedDelay((r, e) -> Duration.ofMillis(10)).done()
                .withFallback().applyOn(TestException.class).handler(this::fallback).done()
                .build()
                .adaptSupplier(this::actionFail);

        assertThat(guarded.get())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");
        assertThat(counter).hasValue(4); // 1 initial invocation + 3 retries
    }

    @Test
    public void asyncRetryWithComputedDelay_untyped() throws Exception {
        Callable<CompletionStage<String>> guarded = Guard.create()
                .withRetry().maxRetries(5).computedDelay((r, e) -> Duration.ofMillis(10)).done()
                .build()
                .adaptCallable(() -> {
                    int value = counter.incrementAndGet();
                    if (value < 4) {
                        throw new TestException();
                    }
                    return completedFuture("success");
                }, Types.CS_STRING);

        assertThat(guarded.call())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("success");
        assertThat(counter).hasValue(4); // 1 initial invocation, 2 unsuccessful retries, 1 successful retry
    }

    @Test
    public void asyncRetryWithComputedDelayOnResult() throws Exception {
        List<Object> seenResults = new ArrayList<>();
        Supplier<CompletionStage<String>> guarded = TypedGuard.create(Types.CS_STRING)
                .withRetry().maxRetries(3).whenResult(r -> !"success".equals(r))
                .computedDelay((result, failure) -> {
                    seenResults.add(result);
                    return Duration.ofMillis(10);
                }).done()
                .build()
                .adaptSupplier(() -> {
                    int value = counter.incrementAndGet();
                    return completedFuture(value < 3 ? "retry-" + value : "success");
                });

        assertThat(guarded.get())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("success");
        assertThat(counter).hasValue(3);
        assertThat(seenResults).containsExactly("retry-1", "retry-2");
    }

    @Test
    public void retryWithComputedDelayOnResult_untyped() throws Exception {
        List<Object> seenResults = new ArrayList<>();
        Callable<CompletionStage<String>> guarded = Guard.create()
                .withRetry().maxRetries(3).whenResult(r -> !"success".equals(r))
                .computedDelay((result, failure) -> {
                    seenResults.add(result);
                    return Duration.ofMillis(10);
                }).done()
                .build()
                .adaptCallable(() -> {
                    int value = counter.incrementAndGet();
                    return completedFuture(value < 3 ? "retry-" + value : "success");
                }, Types.CS_STRING);

        assertThat(guarded.call())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("success");
        assertThat(counter).hasValue(3);
        assertThat(seenResults).containsExactly("retry-1", "retry-2");
    }

    @Test
    public void retryWithComputedDelayOnResultAndFailure() throws Exception {
        List<Object> seenFailures = new ArrayList<>();
        List<Object> seenResults = new ArrayList<>();
        Callable<CompletionStage<String>> guarded = TypedGuard.create(Types.CS_STRING)
                .withRetry().maxRetries(3).whenResult(r -> !"success".equals(r))
                .computedDelay((result, failure) -> {
                    seenResults.add(result);
                    seenFailures.add(failure);
                    return Duration.ofMillis(10);
                }).done()
                .build()
                .adaptCallable(() -> {
                    int value = counter.incrementAndGet();
                    if (value == 1) {
                        return completedFuture("retry");
                    } else if (value == 2) {
                        return failedFuture(new TestException());
                    }
                    return completedFuture("success");
                });

        assertThat(guarded.call())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("success");
        assertThat(counter).hasValue(3);
        assertThat(seenResults).containsExactly("retry", null);
        assertThat(seenFailures).hasSize(2);
        assertThat(seenFailures.get(0)).isNull();
        assertThat(seenFailures.get(1)).isInstanceOf(TestException.class);
    }

    @Test
    public void asyncRetryWithBeforeRetry() {
        List<Object> seenResults = new ArrayList<>();
        List<Object> seenFailures = new ArrayList<>();
        Supplier<CompletionStage<String>> guarded = TypedGuard.create(Types.CS_STRING)
                .withRetry().maxRetries(3)
                .beforeRetry((result, failure) -> {
                    seenResults.add(result);
                    seenFailures.add(failure);
                }).done()
                .withFallback().handler(this::fallback).applyOn(TestException.class).done()
                .build()
                .adaptSupplier(this::actionFail);

        assertThat(guarded.get())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");
        assertThat(counter).hasValue(4); // 1 initial invocation + 3 retries
        assertThat(seenResults).containsExactly(null, null, null);
        assertThat(seenFailures).hasSize(3)
                .allMatch(failure -> failure instanceof TestException);
    }

    @Test
    public void asyncRetryWithBeforeRetryOnResult() throws Exception {
        List<Object> seenResults = new ArrayList<>();
        List<Object> seenFailures = new ArrayList<>();
        Supplier<CompletionStage<String>> guarded = TypedGuard.create(Types.CS_STRING)
                .withRetry().maxRetries(3).whenResult(r -> !"success".equals(r))
                .beforeRetry((result, failure) -> {
                    seenResults.add(result);
                    seenFailures.add(failure);
                }).done()
                .build()
                .adaptSupplier(() -> {
                    int value = counter.incrementAndGet();
                    return completedFuture(value < 3 ? "retry-" + value : "success");
                });

        assertThat(guarded.get())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("success");
        assertThat(counter).hasValue(3);
        // the handler must see the value the CompletionStage produced, not the CompletionStage itself
        assertThat(seenResults).containsExactly("retry-1", "retry-2");
        assertThat(seenFailures).containsExactly(null, null);
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

    @SuppressWarnings("unchecked")
    private <T> T resultOf(Object future) {
        if (future instanceof CompletionStage<?> cs && cs.toCompletableFuture().isDone()) {
            return (T) cs.toCompletableFuture().join();
        }
        throw new IllegalArgumentException("Not a future or not complete: " + future);
    }
}
