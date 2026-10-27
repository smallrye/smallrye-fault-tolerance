package io.smallrye.faulttolerance.apiimpl;

import io.smallrye.faulttolerance.core.metrics.MeteredOperation;

final class BasicMeteredOperationImpl implements MeteredOperation {
    private final boolean enabled;
    private final String name;
    private final boolean mayBeAsynchronous;
    private final boolean hasBulkhead;
    private final int bulkheadLimit;
    private final int bulkheadQueueSize;
    private final boolean hasCircuitBreaker;
    private final boolean hasFallback;
    private final boolean hasRateLimit;
    private final int rateLimitLimit;
    private final long rateLimitWindow;
    private final long rateLimitMinSpacing;
    private final boolean hasRetry;
    private final boolean hasTimeout;
    private final long timeoutDuration;

    BasicMeteredOperationImpl(boolean enabled, String name,
            boolean mayBeAsynchronous,
            boolean hasBulkhead, int bulkheadLimit, int bulkheadQueueSize,
            boolean hasCircuitBreaker,
            boolean hasFallback,
            boolean hasRateLimit, int rateLimitLimit, long rateLimitWindow, long rateLimitMinSpacing,
            boolean hasRetry,
            boolean hasTimeout, long timeoutDuration) {
        this.enabled = enabled;
        this.name = name;
        this.mayBeAsynchronous = mayBeAsynchronous;
        this.hasBulkhead = hasBulkhead;
        this.bulkheadLimit = bulkheadLimit;
        this.bulkheadQueueSize = bulkheadQueueSize;
        this.hasCircuitBreaker = hasCircuitBreaker;
        this.hasFallback = hasFallback;
        this.hasRateLimit = hasRateLimit;
        this.rateLimitLimit = rateLimitLimit;
        this.rateLimitWindow = rateLimitWindow;
        this.rateLimitMinSpacing = rateLimitMinSpacing;
        this.hasRetry = hasRetry;
        this.hasTimeout = hasTimeout;
        this.timeoutDuration = timeoutDuration;
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public boolean mayBeAsynchronous() {
        return mayBeAsynchronous;
    }

    @Override
    public boolean hasBulkhead() {
        return hasBulkhead;
    }

    @Override
    public int bulkheadLimit() {
        return bulkheadLimit;
    }

    @Override
    public int bulkheadQueueSize() {
        return bulkheadQueueSize;
    }

    @Override
    public boolean hasCircuitBreaker() {
        return hasCircuitBreaker;
    }

    @Override
    public boolean hasFallback() {
        return hasFallback;
    }

    @Override
    public boolean hasRateLimit() {
        return hasRateLimit;
    }

    @Override
    public int rateLimitLimit() {
        return rateLimitLimit;
    }

    @Override
    public long rateLimitWindow() {
        return rateLimitWindow;
    }

    @Override
    public long rateLimitMinSpacing() {
        return rateLimitMinSpacing;
    }

    @Override
    public boolean hasRetry() {
        return hasRetry;
    }

    @Override
    public boolean hasTimeout() {
        return hasTimeout;
    }

    @Override
    public long timeoutDuration() {
        return timeoutDuration;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Object cacheKey() {
        return name;
    }
}
