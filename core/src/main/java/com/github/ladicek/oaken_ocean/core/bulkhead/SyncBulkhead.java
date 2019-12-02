package com.github.ladicek.oaken_ocean.core.bulkhead;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.SimpleInvocationContext;

import java.util.concurrent.Semaphore;

/**
 * This class is a one big TODO :)
 */
public class SyncBulkhead<V> extends BulkheadBase<V, SimpleInvocationContext<V>> {
    private final Semaphore bulkheadSemaphore;

    public SyncBulkhead(FaultToleranceStrategy<V, SimpleInvocationContext<V>> delegate, String description, int size, int queueSize,
                        MetricsRecorder metricsRecorder) {
        super(description, delegate, metricsRecorder);
        this.bulkheadSemaphore = new Semaphore(size);
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
