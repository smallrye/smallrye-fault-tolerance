package io.smallrye.faulttolerance.core.bulkhead;

import java.util.concurrent.Semaphore;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.SimpleInvocationContext;

public class SyncBulkhead<V> extends BulkheadBase<V, SimpleInvocationContext<V>> {
    private final Semaphore bulkheadSemaphore;

    public SyncBulkhead(FaultToleranceStrategy<V, SimpleInvocationContext<V>> delegate, String description, int size,
            MetricsRecorder metricsRecorder) {
        super(description, delegate, metricsRecorder);
        bulkheadSemaphore = new Semaphore(size);
    }

    @Override
    public V apply(SimpleInvocationContext<V> target) throws Exception {
        if (bulkheadSemaphore.tryAcquire()) {
            recorder.bulkheadEntered();
            long startTime = System.nanoTime();
            try {
                return delegate.apply(target);
            } finally {
                bulkheadSemaphore.release();
                recorder.bulkheadLeft(System.nanoTime() - startTime);
            }
        } else {
            recorder.bulkheadRejected();
            throw bulkheadRejected();
        }
    }
}
