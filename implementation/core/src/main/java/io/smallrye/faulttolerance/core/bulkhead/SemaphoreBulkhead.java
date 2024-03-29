package io.smallrye.faulttolerance.core.bulkhead;

import static io.smallrye.faulttolerance.core.bulkhead.BulkheadLogger.LOG;

import java.util.concurrent.Semaphore;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;

/**
 * Semaphore style bulkhead for synchronous executions.
 */
public class SemaphoreBulkhead<V> extends BulkheadBase<V> {
    private final Semaphore semaphore;

    public SemaphoreBulkhead(FaultToleranceStrategy<V> delegate, String description, int size) {
        super(description, delegate);
        this.semaphore = new Semaphore(size);
    }

    @Override
    public V apply(InvocationContext<V> ctx) throws Exception {
        LOG.trace("SemaphoreBulkhead started");
        try {
            return doApply(ctx);
        } finally {
            LOG.trace("SemaphoreBulkhead finished");
        }
    }

    private V doApply(InvocationContext<V> ctx) throws Exception {
        if (semaphore.tryAcquire()) {
            LOG.trace("Semaphore acquired, accepting task into bulkhead");
            ctx.fireEvent(BulkheadEvents.DecisionMade.ACCEPTED);
            ctx.fireEvent(BulkheadEvents.StartedRunning.INSTANCE);
            try {
                return delegate.apply(ctx);
            } finally {
                semaphore.release();
                LOG.trace("Semaphore released, task leaving bulkhead");
                ctx.fireEvent(BulkheadEvents.FinishedRunning.INSTANCE);
            }
        } else {
            LOG.debugOrTrace(description + " invocation prevented by bulkhead",
                    "Semaphore not acquired, rejecting task from bulkhead");
            ctx.fireEvent(BulkheadEvents.DecisionMade.REJECTED);
            throw bulkheadRejected();
        }
    }
}
