package io.smallrye.faulttolerance.core.timeout;

import static io.smallrye.faulttolerance.core.util.Preconditions.check;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;

public abstract class TimeoutBase<V, ContextType extends InvocationContext<V>>
        implements FaultToleranceStrategy<V, ContextType> {
    final FaultToleranceStrategy<V, ContextType> delegate;
    final String description;

    final long timeoutInMillis;
    final TimeoutWatcher watcher;
    final MetricsRecorder metricsRecorder;

    TimeoutBase(FaultToleranceStrategy<V, ContextType> delegate, String description, long timeoutInMillis,
            TimeoutWatcher watcher,
            MetricsRecorder metricsRecorder) {
        this.delegate = checkNotNull(delegate, "Timeout delegate must be set");
        this.description = checkNotNull(description, "Timeout description must be set");
        this.timeoutInMillis = check(timeoutInMillis, timeoutInMillis > 0, "Timeout must be > 0");
        this.watcher = checkNotNull(watcher, "Timeout watcher must be set");
        this.metricsRecorder = metricsRecorder == null ? MetricsRecorder.NO_OP : metricsRecorder;
    }

    TimeoutException timeoutException() {
        return new TimeoutException(description + " timed out");
    }

    public interface MetricsRecorder {
        void timeoutSucceeded(long time);

        void timeoutTimedOut(long time);

        void timeoutFailed(long time);

        MetricsRecorder NO_OP = new MetricsRecorder() {
            @Override
            public void timeoutSucceeded(long time) {
            }

            @Override
            public void timeoutTimedOut(long time) {
            }

            @Override
            public void timeoutFailed(long time) {
            }
        };
    }
}
