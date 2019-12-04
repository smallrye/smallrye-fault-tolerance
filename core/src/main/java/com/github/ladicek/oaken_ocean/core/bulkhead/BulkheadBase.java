package com.github.ladicek.oaken_ocean.core.bulkhead;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.InvocationContext;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public abstract class BulkheadBase<V, ContextType extends InvocationContext<V>>
        implements FaultToleranceStrategy<V, ContextType> {
    private final String description;
    final FaultToleranceStrategy<V, ContextType> delegate;
    final SyncBulkhead.MetricsRecorder recorder;

    BulkheadBase(String description, FaultToleranceStrategy<V, ContextType> delegate, MetricsRecorder recorder) {
        this.description = description;
        this.delegate = delegate;
        this.recorder = recorder == null ? MetricsRecorder.NOOP : recorder;
    }

    BulkheadException bulkheadRejected() {
        return new BulkheadException(description + " rejected from bulkhead");
    }

    public interface MetricsRecorder {
        void bulkheadQueueEntered();

        void bulkheadQueueLeft(long timeInQueue);

        void bulkheadEntered();

        void bulkheadRejected();

        void bulkheadLeft(long processingTime);

        MetricsRecorder NOOP = new MetricsRecorder() {
            @Override
            public void bulkheadQueueEntered() {
            }

            @Override
            public void bulkheadQueueLeft(long timeInQueue) {
            }

            @Override
            public void bulkheadEntered() {
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
