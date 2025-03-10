package io.smallrye.faulttolerance.metrics;

import io.smallrye.faulttolerance.SpecCompatibility;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;
import io.smallrye.faulttolerance.core.metrics.MeteredOperation;
import io.smallrye.faulttolerance.internal.InterceptionPoint;

public final class CdiMeteredOperationImpl implements MeteredOperation {
    private final FaultToleranceOperation operation;
    private final InterceptionPoint interceptionPoint;
    private final SpecCompatibility specCompatibility;

    public CdiMeteredOperationImpl(FaultToleranceOperation operation, InterceptionPoint interceptionPoint,
            SpecCompatibility specCompatibility) {
        this.operation = operation;
        this.interceptionPoint = interceptionPoint;
        this.specCompatibility = specCompatibility;
    }

    @Override
    public boolean enabled() {
        // always enabled, because this only applies to intercepted methods
        return true;
    }

    @Override
    public boolean mayBeAsynchronous() {
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

    @Override
    public Object cacheKey() {
        return interceptionPoint;
    }
}
