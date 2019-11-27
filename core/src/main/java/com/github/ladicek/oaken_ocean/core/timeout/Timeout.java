package com.github.ladicek.oaken_ocean.core.timeout;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

import java.util.concurrent.Callable;

import static com.github.ladicek.oaken_ocean.core.util.Preconditions.check;
import static com.github.ladicek.oaken_ocean.core.util.Preconditions.checkNotNull;

public class Timeout<V> implements FaultToleranceStrategy<V> {
    final FaultToleranceStrategy<V> delegate;
    final String description;

    final long timeoutInMillis;
    final TimeoutWatcher watcher;
    final MetricsRecorder metricsRecorder;

    public Timeout(FaultToleranceStrategy<V> delegate, String description, long timeoutInMillis, TimeoutWatcher watcher,
                   MetricsRecorder metricsRecorder) {
        this.delegate = checkNotNull(delegate, "Timeout delegate must be set");
        this.description = checkNotNull(description, "Timeout description must be set");
        this.timeoutInMillis = check(timeoutInMillis, timeoutInMillis > 0, "Timeout must be > 0");
        this.watcher = checkNotNull(watcher, "Timeout watcher must be set");
        this.metricsRecorder = metricsRecorder == null ? MetricsRecorder.NO_OP : metricsRecorder;
    }

    @Override
    public V apply(Callable<V> target) throws Exception {
        TimeoutExecution execution = new TimeoutExecution(Thread.currentThread(), timeoutInMillis);
        TimeoutWatch watch = watcher.schedule(execution);
        long start = System.nanoTime();

        V result = null;
        Exception exception = null;
        boolean interrupted = false;
        try {
            result = delegate.apply(target);
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
            throw new InterruptedException();
        }

        if (execution.hasTimedOut()) {
            long end = System.nanoTime();
            metricsRecorder.timeoutTimedOut(end - start);
            throw timeoutException();
        }

        if (exception != null) {
            throw exception;
        }
        long end = System.nanoTime();
        metricsRecorder.timeoutSucceeded(end - start);

        return result;
    }

    TimeoutException timeoutException() {
        return new TimeoutException(description + " timed out");
    }

    public interface MetricsRecorder {
        void timeoutSucceeded(long time);
        void timeoutTimedOut(long time);

        MetricsRecorder NO_OP = new MetricsRecorder() {
            @Override
            public void timeoutSucceeded(long time) {
            }

            @Override
            public void timeoutTimedOut(long time) {
            }
        };
    }
}
