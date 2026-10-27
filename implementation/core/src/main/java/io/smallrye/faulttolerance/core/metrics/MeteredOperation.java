package io.smallrye.faulttolerance.core.metrics;

public interface MeteredOperation {
    boolean enabled();

    boolean mayBeAsynchronous();

    boolean hasBulkhead();

    int bulkheadLimit();

    int bulkheadQueueSize();

    boolean hasCircuitBreaker();

    long circuitBreakerDelay();

    int circuitBreakerRequestVolumeThreshold();

    double circuitBreakerFailureRatio();

    int circuitBreakerSuccessThreshold();

    boolean hasFallback();

    boolean hasRateLimit();

    int rateLimitLimit();

    long rateLimitWindow();

    long rateLimitMinSpacing();

    boolean hasRetry();

    boolean hasTimeout();

    long timeoutDuration();

    String name();

    Object cacheKey();
}
