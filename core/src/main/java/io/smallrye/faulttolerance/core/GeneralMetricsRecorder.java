package io.smallrye.faulttolerance.core;

// TODO better name?
public class GeneralMetricsRecorder<V> implements FaultToleranceStrategy<V> {
    private final FaultToleranceStrategy<V> delegate;
    private final GeneralMetrics metrics;

    public GeneralMetricsRecorder(FaultToleranceStrategy<V> delegate, GeneralMetrics metrics) {
        this.delegate = delegate;
        this.metrics = metrics;
    }

    @Override
    public V apply(InvocationContext<V> ctx) throws Exception {
        metrics.invoked();
        try {
            return delegate.apply(ctx);
        } catch (Exception e) {
            metrics.failed();
            throw e;
        }
    }
}
