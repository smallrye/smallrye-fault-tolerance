package io.smallrye.faulttolerance.apiimpl;

import io.smallrye.faulttolerance.core.metrics.MeteredOperation;

final class BasicMeteredOperationImpl implements MeteredOperation {
    private final String name;
    private final boolean mayBeAsynchronous;
    private final boolean hasBulkhead;
    private final boolean hasCircuitBreaker;
    private final boolean hasFallback;
    private final boolean hasRateLimit;
    private final boolean hasRetry;
    private final boolean hasTimeout;

    BasicMeteredOperationImpl(String name, boolean mayBeAsynchronous, boolean hasBulkhead, boolean hasCircuitBreaker,
            boolean hasFallback, boolean hasRateLimit, boolean hasRetry, boolean hasTimeout) {
        this.name = name;
        this.mayBeAsynchronous = mayBeAsynchronous;
        this.hasBulkhead = hasBulkhead;
        this.hasCircuitBreaker = hasCircuitBreaker;
        this.hasFallback = hasFallback;
        this.hasRateLimit = hasRateLimit;
        this.hasRetry = hasRetry;
        this.hasTimeout = hasTimeout;
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
    public boolean hasRetry() {
        return hasRetry;
    }

    @Override
    public boolean hasTimeout() {
        return hasTimeout;
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
