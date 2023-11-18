package io.smallrye.faulttolerance.core.timeout;

import static io.smallrye.faulttolerance.core.timeout.TimeoutLogger.LOG;
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

    public Timeout(FaultToleranceStrategy<V> delegate, String description, long timeoutInMillis, TimeoutWatcher watcher) {
        this.delegate = checkNotNull(delegate, "Timeout delegate must be set");
        this.description = checkNotNull(description, "Timeout description must be set");
        this.timeoutInMillis = check(timeoutInMillis, timeoutInMillis > 0, "Timeout must be > 0");
        this.watcher = checkNotNull(watcher, "Timeout watcher must be set");
    }

    @Override
    public V apply(InvocationContext<V> ctx) throws Exception {
        LOG.trace("Timeout started");
        try {
            return doApply(ctx);
        } finally {
            LOG.trace("Timeout finished");
        }
    }

    private V doApply(InvocationContext<V> ctx) throws Exception {
        // must extract `AsyncTimeoutNotification` early, because if retries are present,
        // a different `AsyncTimeoutNotification` may be present in the `InvocationContext`
        // by the time the timeout callback is invoked
        AsyncTimeoutNotification notification = ctx.get(AsyncTimeoutNotification.class);
        ctx.remove(AsyncTimeoutNotification.class);

        TimeoutExecution execution = new TimeoutExecution(Thread.currentThread(), timeoutInMillis, () -> {
            if (notification != null) {
                notification.accept(timeoutException(description));
            }
        });
        var watch = watcher.schedule(execution);
        ctx.fireEvent(TimeoutEvents.Started.INSTANCE);

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

        if (execution.hasTimedOut()) {
            LOG.debugf("%s invocation timed out (%d ms)", description, timeoutInMillis);
            ctx.fireEvent(TimeoutEvents.Finished.TIMED_OUT);
            throw timeoutException(description);
        }

        ctx.fireEvent(TimeoutEvents.Finished.NORMALLY);

        if (exception != null) {
            throw exception;
        }

        return result;
    }

    static TimeoutException timeoutException(String description) {
        return new TimeoutException(description + " timed out");
    }
}
