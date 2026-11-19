package io.smallrye.faulttolerance.retry.computed.delay;

import java.time.Duration;

public class RetryAfterResult {
    private final boolean retry;
    private final Duration retryAfter;

    public RetryAfterResult(boolean retry, Duration retryAfter) {
        this.retry = retry;
        this.retryAfter = retryAfter;
    }

    public boolean shouldRetry() {
        return retry;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
