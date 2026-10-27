package io.smallrye.faulttolerance.core.metrics;

public interface MeteredOperation {
    boolean enabled();

    boolean mayBeAsynchronous();

    boolean hasBulkhead();

    int bulkheadLimit();

    int bulkheadQueueSize();

    boolean hasCircuitBreaker();

    boolean hasFallback();

    boolean hasRateLimit();

    boolean hasRetry();

    boolean hasTimeout();

    long timeoutDuration();

    String name();

    Object cacheKey();
}
