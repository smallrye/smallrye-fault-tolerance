package io.smallrye.faulttolerance.core.timeout;

import static io.smallrye.faulttolerance.core.timeout.TimeoutLogger.LOG;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;
import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;

/**
 * The next strategy in the chain must be {@link Timeout}, and it is invoked on an extra thread.
 * Communication then happens using {@link AsyncTimeoutNotification}.
 * <p>
 * Note that the {@code TimeoutException} thrown from this strategy might come from two places:
 * the {@code Timeout} strategy throwing, or {@code AsyncTimeoutTask#timedOut} setting an exception
 * as a result of {@code AsyncTimeoutNotification}. Both might happen, and whichever happens first
 * gets to decide.
 */
public class AsyncTimeout<V> implements FaultToleranceStrategy<Future<V>> {
    private final FaultToleranceStrategy<Future<V>> delegate;
    private final Executor executor;

    public AsyncTimeout(Timeout<Future<V>> delegate, Executor executor) {
        this.delegate = delegate;
        this.executor = checkNotNull(executor, "Executor must be set");
    }

    @Override
    public Future<V> apply(InvocationContext<Future<V>> ctx) throws Exception {
        LOG.trace("AsyncTimeout started");
        try {
            return doApply(ctx);
        } finally {
            LOG.trace("AsyncTimeout finished");
        }
    }

    private Future<V> doApply(InvocationContext<Future<V>> ctx) throws Exception {
        AsyncTimeoutTask<Future<V>> task = new AsyncTimeoutTask<>(() -> delegate.apply(ctx));
        LOG.asyncTimeoutTaskCreated(task);
        ctx.set(AsyncTimeoutNotification.class, task::timedOut);

        executor.execute(task);
        try {
            return task.get();
        } catch (ExecutionException e) {
            LOG.asyncTimeoutRethrowing(e.getCause());
            throw sneakyThrow(e.getCause());
        }
    }

    // only to expose `setException`, which is `protected` in `FutureTask`
    private static class AsyncTimeoutTask<T> extends FutureTask<T> {
        AsyncTimeoutTask(Callable<T> callable) {
            super(callable);
        }

        public void timedOut(TimeoutException exception) {
            LOG.asyncTimeoutTaskCompleting(this, exception);
            super.setException(exception);
        }
    }
}
