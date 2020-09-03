package io.smallrye.faulttolerance.core.bulkhead;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;

public abstract class BulkheadBase<V> implements FaultToleranceStrategy<V> {
    private final String description;
    final FaultToleranceStrategy<V> delegate;

    BulkheadBase(String description, FaultToleranceStrategy<V> delegate) {
        this.description = description;
        this.delegate = delegate;
    }

    final BulkheadException bulkheadRejected() {
        return new BulkheadException(description + " rejected from bulkhead");
    }
}
