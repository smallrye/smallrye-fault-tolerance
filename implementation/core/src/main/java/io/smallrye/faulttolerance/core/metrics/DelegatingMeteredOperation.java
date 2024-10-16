package io.smallrye.faulttolerance.core.metrics;

final class DelegatingMeteredOperation implements MeteredOperation {
    private final MeteredOperation operation;
    private final String name;

    DelegatingMeteredOperation(MeteredOperation operation, String name) {
        this.operation = operation;
        this.name = name;
    }

    @Override
    public boolean isAsynchronous() {
        return operation.isAsynchronous();
    }

    @Override
    public boolean hasBulkhead() {
        return operation.hasBulkhead();
    }

    @Override
    public boolean hasCircuitBreaker() {
        return operation.hasCircuitBreaker();
    }

    @Override
    public boolean hasFallback() {
        return operation.hasFallback();
    }

    @Override
    public boolean hasRateLimit() {
        return operation.hasRateLimit();
    }

    @Override
    public boolean hasRetry() {
        return operation.hasRetry();
    }

    @Override
    public boolean hasTimeout() {
        return operation.hasTimeout();
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
