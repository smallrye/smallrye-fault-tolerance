package io.smallrye.faulttolerance.retry.computed.delay;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.ComputedDelay;
import io.smallrye.faulttolerance.api.ExponentialBackoff;

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

    @Retry(maxRetries = 3, delay = 50, jitter = 0)
    @ComputedDelay(MyComputedDelayHandler.class)
    public void retryWithExceptionInHandler() {
        invocationTimes.add(System.currentTimeMillis());
        if (attempts.incrementAndGet() <= 3) {
            // handler throws exception: falls back to configured delay (50ms)
            throw new IllegalStateException("trigger-handler-exception");
        }
    }
}
