package com.github.ladicek.oaken_ocean.core.timeout;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.SimpleInvocationContext;

public class SyncTimeout<V> extends TimeoutBase<V, SimpleInvocationContext<V>> {

    public SyncTimeout(FaultToleranceStrategy<V, SimpleInvocationContext<V>> delegate,
                       String description,
                       long timeoutInMillis,
                       TimeoutWatcher watcher,
                       MetricsRecorder metricsRecorder) {
        super(delegate, description, timeoutInMillis, watcher, metricsRecorder);
    }

    @Override
    public V apply(SimpleInvocationContext<V> target) throws Exception {
        TimeoutExecution execution = new TimeoutExecution(Thread.currentThread(), null, timeoutInMillis);
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
            exception = new InterruptedException();
        }

        long end = System.nanoTime();
        if (execution.hasTimedOut()) {
            metricsRecorder.timeoutTimedOut(end - start);
            throw timeoutException();
        }

        if (exception != null) {
            metricsRecorder.timeoutFailed(end - start);
            throw exception;
        }

        metricsRecorder.timeoutSucceeded(end - start);

        return result;
    }
}
