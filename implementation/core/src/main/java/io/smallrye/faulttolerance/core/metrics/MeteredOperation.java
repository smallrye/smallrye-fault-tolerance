package io.smallrye.faulttolerance.core.metrics;

public interface MeteredOperation {
    boolean mayBeAsynchronous();

    boolean hasBulkhead();

    boolean hasCircuitBreaker();

    boolean hasFallback();

    boolean hasRateLimit();

    boolean hasRetry();

    boolean hasTimeout();

    String name();

    Object cacheKey();
}
