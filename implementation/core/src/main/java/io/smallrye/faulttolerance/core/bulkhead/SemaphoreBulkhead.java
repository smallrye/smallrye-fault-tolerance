package io.smallrye.faulttolerance.core.bulkhead;

import java.util.concurrent.Semaphore;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;

public class SemaphoreBulkhead<V> extends BulkheadBase<V> {
    private final Semaphore semaphore;

    public SemaphoreBulkhead(FaultToleranceStrategy<V> delegate, String description, int size,
            MetricsRecorder metricsRecorder) {
        super(description, delegate, metricsRecorder);
        semaphore = new Semaphore(size);
    }

    @Override
    public V apply(InvocationContext<V> ctx) throws Exception {
        if (semaphore.tryAcquire()) {
            recorder.bulkheadEntered();
            long startTime = System.nanoTime();
            try {
                return delegate.apply(ctx);
            } finally {
                semaphore.release();
                recorder.bulkheadLeft(System.nanoTime() - startTime);
            }
        } else {
            recorder.bulkheadRejected();
            throw bulkheadRejected();
        }
    }
}
