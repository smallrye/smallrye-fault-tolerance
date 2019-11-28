package com.github.ladicek.oaken_ocean.core.bulkhead;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

/**
 * This class is a one big TODO :)
 */
public class Bulkhead<V> implements FaultToleranceStrategy<V> {
    final FaultToleranceStrategy<V> delegate;
    private final String description;

    private final Semaphore bulkheadSemaphore;
    private final MetricsRecorder recorder;

    public Bulkhead(FaultToleranceStrategy<V> delegate, String description, int size,
                    MetricsRecorder metricsRecorder) {
        this.delegate = delegate;
        this.description = description;
        this.bulkheadSemaphore = new Semaphore(size);
        this.recorder = metricsRecorder == null ? MetricsRecorder.NOOP : metricsRecorder;
    }

    @Override
    public V apply(Callable<V> target) throws Exception {
        recorder.bulkheadQueueEntered(); // mstodo needed or not?
        if (bulkheadSemaphore.tryAcquire()) {
            recorder.bulkheadEntered(0L); // mstodo do it only for async execution
            long startTime = System.nanoTime();
            try {
                return delegate.apply(target);
            } finally {
                bulkheadSemaphore.release();
                recorder.bulkheadLeft(System.nanoTime() - startTime);
            }
        } else {
            recorder.bulkheadRejected();
            throw new BulkheadException(); // mstodo
        }
    }

    public interface MetricsRecorder {
        void bulkheadQueueEntered();
        void bulkheadEntered(long timeInQueue);
        void bulkheadRejected();
        void bulkheadLeft(long processingTime);

        MetricsRecorder NOOP = new MetricsRecorder() {
            @Override
            public void bulkheadQueueEntered() {
            }

            @Override
            public void bulkheadEntered(long timeInQueue) {
            }

            @Override
            public void bulkheadRejected() {
            }

            @Override
            public void bulkheadLeft(long processingTime) {
            }
        };
    }
}
