package io.smallrye.faulttolerance.retry.computed.delay;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.ComputedDelay;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import io.smallrye.faulttolerance.api.RetryWhen;

@Dependent
public class ComputedDelayService {
    static final List<Long> invocationTimes = new ArrayList<>();
    static final AtomicInteger attempts = new AtomicInteger();

    @Retry(maxRetries = 3, delay = 0, jitter = 0)
    @ComputedDelay(MyComputedDelayHandler.class)
    public void retryWithComputedDelay() {
        invocationTimes.add(System.currentTimeMillis());
        if (attempts.incrementAndGet() <= 3) {
            throw new RetryAfterException(Duration.ofMillis(150));
        }
    }

    @Retry(maxRetries = 3, delay = 50, jitter = 0)
    @ExponentialBackoff
    @ComputedDelay(MyComputedDelayHandler.class)
    public void retryWithFallbackToExponential() {
        invocationTimes.add(System.currentTimeMillis());
        if (attempts.incrementAndGet() <= 3) {
            // null delay: falls back to ExponentialBackoff (50ms, 100ms, 200ms)
            throw new RetryAfterException(null);
        }
    }

    @Retry(maxRetries = 3, delay = 50, jitter = 0)
    @ComputedDelay(MyComputedDelayHandler.class)
    public void retryWithNegativeDelay() {
        invocationTimes.add(System.currentTimeMillis());
        if (attempts.incrementAndGet() <= 3) {
            // negative delay: falls back to configured delay (50ms)
            throw new RetryAfterException(Duration.ofMillis(-100));
        }
    }

    @Retry(maxRetries = 3, delay = 0, jitter = 0)
    @RetryWhen(result = IsRetryAfterResult.class)
    @ComputedDelay(MyComputedDelayHandler.class)
    public RetryAfterResult retryOnResultWithComputedDelay() {
        invocationTimes.add(System.currentTimeMillis());
        if (attempts.incrementAndGet() <= 3) {
            return new RetryAfterResult(true, Duration.ofMillis(150));
        }
        return new RetryAfterResult(false, null);
    }

    @Retry(maxRetries = 3, delay = 0, jitter = 0)
    @RetryWhen(result = IsRetryAfterResult.class)
    @ComputedDelay(MyComputedDelayHandler.class)
    public RetryAfterResult retryOnResultOrExceptionWithComputedDelay() {
        invocationTimes.add(System.currentTimeMillis());
        int attempt = attempts.incrementAndGet();
        if (attempt == 1 || attempt == 3) {
            return new RetryAfterResult(true, Duration.ofMillis(150));
        } else if (attempt == 2) {
            throw new RetryAfterException(Duration.ofMillis(200));
        }
        return new RetryAfterResult(false, null);
    }

    /**
     * Returns {@code 0} when the action succeeded, otherwise the delay in milliseconds to wait before retrying.
     * The return type is a primitive, so the computed delay handler must see a boxed {@code Integer}.
     */
    @Retry(maxRetries = 3, delay = 0, jitter = 0)
    @RetryWhen(result = IsNonZero.class)
    @ComputedDelay(MyComputedDelayHandler.class)
    public int retryOnPrimitiveResultWithComputedDelay() {
        invocationTimes.add(System.currentTimeMillis());
        int attempt = attempts.incrementAndGet();
        return attempt < 4 ? 150 : 0;
    }

    @Retry(maxRetries = 3, delay = 50, jitter = 0)
    @ComputedDelay(MyComputedDelayHandler.class)
    public void retryWithExceptionInHandler() {
        invocationTimes.add(System.currentTimeMillis());
        if (attempts.incrementAndGet() <= 3) {
            // handler throws exception: falls back to configured delay (50ms)
            throw new IllegalStateException("trigger-handler-exception");
        }
    }

    @Asynchronous
    @Retry(maxRetries = 3, delay = 0, jitter = 0)
    @RetryWhen(result = IsRetryAfterResult.class)
    @ComputedDelay(MyComputedDelayHandler.class)
    public CompletionStage<RetryAfterResult> asyncRetryOnResultWithComputedDelay() {
        invocationTimes.add(System.currentTimeMillis());
        if (attempts.incrementAndGet() <= 3) {
            return completedFuture(new RetryAfterResult(true, Duration.ofMillis(150)));
        }
        return completedFuture(new RetryAfterResult(false, null));
    }

    @Asynchronous
    @Retry(maxRetries = 3, delay = 0, jitter = 0)
    @ComputedDelay(MyComputedDelayHandler.class)
    public CompletionStage<RetryAfterResult> asyncRetryOnFailureWithComputedDelay() {
        invocationTimes.add(System.currentTimeMillis());
        if (attempts.incrementAndGet() <= 3) {
            return failedFuture(new RetryAfterException(Duration.ofMillis(150)));
        }
        return completedFuture(new RetryAfterResult(false, null));
    }
}
