package io.smallrye.faulttolerance.retry.computed.delay;

import java.time.Duration;

public class RetryAfterException extends RuntimeException {
    private final Duration retryAfter;

    public RetryAfterException(Duration retryAfter) {
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
