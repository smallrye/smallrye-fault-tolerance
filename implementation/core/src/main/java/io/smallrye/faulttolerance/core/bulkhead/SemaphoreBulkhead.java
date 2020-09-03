package io.smallrye.faulttolerance.core.bulkhead;

import java.util.concurrent.Semaphore;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;

public class SemaphoreBulkhead<V> extends BulkheadBase<V> {
    private final Semaphore semaphore;

    public SemaphoreBulkhead(FaultToleranceStrategy<V> delegate, String description, int size) {
        super(description, delegate);
        this.semaphore = new Semaphore(size);
    }

    @Override
    public V apply(InvocationContext<V> ctx) throws Exception {
        if (semaphore.tryAcquire()) {
            ctx.fireEvent(BulkheadEvents.DecisionMade.ACCEPTED);
            ctx.fireEvent(BulkheadEvents.StartedRunning.INSTANCE);
            try {
                return delegate.apply(ctx);
            } finally {
                semaphore.release();
                ctx.fireEvent(BulkheadEvents.FinishedRunning.INSTANCE);
            }
        } else {
            ctx.fireEvent(BulkheadEvents.DecisionMade.REJECTED);
            throw bulkheadRejected();
        }
    }
}
