package io.smallrye.faulttolerance.core.async;

import static io.smallrye.faulttolerance.core.async.AsyncLogger.LOG;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;
import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;

public class FutureExecution<V> implements FaultToleranceStrategy<Future<V>> {
    private final FaultToleranceStrategy<Future<V>> delegate;
    private final Executor executor;

    public FutureExecution(FaultToleranceStrategy<Future<V>> delegate, Executor executor) {
        this.delegate = delegate;
        this.executor = checkNotNull(executor, "Executor must be set");
    }

    @Override
    public Future<V> apply(InvocationContext<Future<V>> ctx) {
        LOG.trace("FutureExecution started");
        try {
            return doApply(ctx);
        } finally {
            LOG.trace("FutureExecution finished");
        }
    }

    private Future<V> doApply(InvocationContext<Future<V>> ctx) {
        FutureTask<Future<V>> task = new FutureTask<>(() -> delegate.apply(ctx));
        executor.execute(task);
        return new Future<V>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                ctx.fireEvent(mayInterruptIfRunning
                        ? FutureCancellationEvent.INTERRUPTIBLE
                        : FutureCancellationEvent.NONINTERRUPTIBLE);
                return task.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return task.isCancelled();
            }

            @Override
            public boolean isDone() {
                try {
                    return task.isDone() && (task.isCancelled() || task.get().isDone());
                } catch (InterruptedException e) {
                    // at this point, `task.get` shouldn't block, as `task.isDone` is already true
                    throw sneakyThrow(e);
                } catch (CancellationException | ExecutionException e) {
                    // only happens in `task.get`, which is only called when `task.isDone` is true
                    return true;
                }
            }

            @Override
            public V get() throws InterruptedException, ExecutionException {
                return task.get().get();
            }

            @Override
            public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                // at worst, the timeout here could possibly be 2x the requested value
                return task.get(timeout, unit).get(timeout, unit);
            }
        };
    }
}
