package io.smallrye.faulttolerance.metrics;

import io.smallrye.faulttolerance.SpecCompatibility;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;
import io.smallrye.faulttolerance.core.metrics.MeteredOperation;

public final class MeteredOperationImpl implements MeteredOperation {
    private final FaultToleranceOperation operation;
    private final SpecCompatibility specCompatibility;

    public MeteredOperationImpl(FaultToleranceOperation operation, SpecCompatibility specCompatibility) {
        this.operation = operation;
        this.specCompatibility = specCompatibility;
    }

    @Override
    public boolean isAsynchronous() {
        return specCompatibility.isOperationTrulyOrPseudoAsynchronous(operation);
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
        return operation.getName();
    }
}
