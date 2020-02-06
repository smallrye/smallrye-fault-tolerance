package io.smallrye.faulttolerance.core.fallback;

import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;
import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;

public class Fallback<V> implements FaultToleranceStrategy<V> {
    final FaultToleranceStrategy<V> delegate;
    final String description;

    final FallbackFunction<V> fallback;
    final SetOfThrowables applyOn;
    final SetOfThrowables skipOn;
    final MetricsRecorder metricsRecorder;

    public Fallback(FaultToleranceStrategy<V> delegate, String description, FallbackFunction<V> fallback,
            SetOfThrowables applyOn, SetOfThrowables skipOn, MetricsRecorder metricsRecorder) {
        this.delegate = checkNotNull(delegate, "Fallback delegate must be set");
        this.description = checkNotNull(description, "Fallback description must be set");
        this.fallback = checkNotNull(fallback, "Fallback function must be set");
        this.applyOn = checkNotNull(applyOn, "Set of apply-on throwables must be set");
        this.skipOn = checkNotNull(skipOn, "Set of skip-on throwables must be set");
        this.metricsRecorder = metricsRecorder == null ? MetricsRecorder.NO_OP : metricsRecorder;
    }

    @Override
    public V apply(InvocationContext<V> ctx) throws Exception {
        Throwable failure;
        try {
            return delegate.apply(ctx);
        } catch (Exception e) {
            failure = e;
        }

        if (failure instanceof InterruptedException || Thread.interrupted()) {
            throw new InterruptedException();
        }

        if (shouldSkipFallback(failure)) {
            sneakyThrow(failure);
        }

        metricsRecorder.fallbackCalled();
        return fallback.call(failure);
    }

    boolean shouldSkipFallback(Throwable e) {
        return skipOn.includes(e.getClass()) || !applyOn.includes(e.getClass());
    }

    public interface MetricsRecorder {
        void fallbackCalled();

        MetricsRecorder NO_OP = () -> {
        };
    }
}
