package io.smallrye.faulttolerance.core.metrics;

final class DelegatingMeteredOperation implements MeteredOperation {
    private final MeteredOperation operation;
    private final MeteredOperationMetadata metadata;

    DelegatingMeteredOperation(MeteredOperation operation, MeteredOperationMetadata metadata) {
        this.operation = operation;
        this.metadata = metadata;
    }

    @Override
    public boolean enabled() {
        // always enabled, because this class is only instantiated for intercepted methods
        return true;
    }

    @Override
    public boolean mayBeAsynchronous() {
        return operation.mayBeAsynchronous();
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
        return metadata.hasFallback();
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
        return metadata.name();
    }

    @Override
    public Object cacheKey() {
        return metadata.name();
    }
}
