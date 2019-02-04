package com.github.ladicek.oaken_ocean.core.timeout;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

import java.util.concurrent.Callable;

import static com.github.ladicek.oaken_ocean.core.util.Preconditions.check;
import static com.github.ladicek.oaken_ocean.core.util.Preconditions.checkNotNull;

public class Timeout<V> implements Callable<V> {
    private final Callable<V> delegate;
    private final String description;

    private final long timeoutInMillis;
    private final TimeoutWatcher watcher;

    public Timeout(Callable<V> delegate, String description, long timeoutInMillis, TimeoutWatcher watcher) {
        this.delegate = checkNotNull(delegate, "Timeout action must be set");
        this.description = checkNotNull(description, "Timeout action description must be set");
        this.timeoutInMillis = check(timeoutInMillis, timeoutInMillis > 0, "Timeout must be > 0");
        this.watcher = checkNotNull(watcher, "Timeout watcher must be set");
    }

    @Override
    public V call() throws Exception {
        TimeoutExecution execution = new TimeoutExecution(Thread.currentThread(), timeoutInMillis);
        watcher.schedule(execution);
        // TODO ability to cancel scheduled watch inside/after execution.finish() would be nice to conserve resources

        V result = null;
        Exception exception = null;
        boolean interrupted = false;
        try {
            result = delegate.call();
            execution.finish();
        } catch (InterruptedException e) {
            interrupted = true;
        } catch (Exception e) {
            exception = e;
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
            throw new TimeoutException(description + " timed out");
        }

        if (exception != null) {
            throw exception;
        }

        return result;
    }
}
