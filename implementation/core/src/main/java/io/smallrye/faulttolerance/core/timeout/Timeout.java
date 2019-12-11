package io.smallrye.faulttolerance.core.timeout;

import static io.smallrye.faulttolerance.core.util.Preconditions.check;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;

public class Timeout<V> implements FaultToleranceStrategy<V> {
    final FaultToleranceStrategy<V> delegate;
    final String description;

    final long timeoutInMillis;
    final TimeoutWatcher watcher;
    final MetricsRecorder metricsRecorder;

    public Timeout(FaultToleranceStrategy<V> delegate, String description, long timeoutInMillis,
            TimeoutWatcher watcher, MetricsRecorder metricsRecorder) {
        this.delegate = checkNotNull(delegate, "Timeout delegate must be set");
        this.description = checkNotNull(description, "Timeout description must be set");
        this.timeoutInMillis = check(timeoutInMillis, timeoutInMillis > 0, "Timeout must be > 0");
        this.watcher = checkNotNull(watcher, "Timeout watcher must be set");
        this.metricsRecorder = metricsRecorder == null ? MetricsRecorder.NO_OP : metricsRecorder;
    }

    @Override
    public V apply(InvocationContext<V> ctx) throws Exception {
        TimeoutExecution execution = new TimeoutExecution(Thread.currentThread(),
                () -> ctx.fireEvent(InvocationContext.Event.TIMEOUT), timeoutInMillis);
        TimeoutWatch watch = watcher.schedule(execution);
        long start = System.nanoTime();

        V result = null;
        Exception exception = null;
        boolean interrupted = false;
        try {
            result = delegate.apply(ctx);
        } catch (InterruptedException e) {
            interrupted = true;
        } catch (Exception e) {
            exception = e;
        } finally {
            // if the execution already timed out, this will be a noop
            execution.finish(watch::cancel);
        }

        if (Thread.interrupted()) {
            // using `Thread.interrupted()` intentionally and unconditionally, because per MP FT spec, chapter 6.1,
            // interruption status must be cleared when the method returns
            interrupted = true;
        }

        if (interrupted && !execution.hasTimedOut()) {
            exception = new InterruptedException();
        }

        long end = System.nanoTime();
        if (execution.hasTimedOut()) {
            metricsRecorder.timeoutTimedOut(end - start);
            throw timeoutException(description);
        }

        if (exception != null) {
            metricsRecorder.timeoutFailed(end - start);
            throw exception;
        }

        metricsRecorder.timeoutSucceeded(end - start);

        return result;
    }

    static TimeoutException timeoutException(String description) {
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
