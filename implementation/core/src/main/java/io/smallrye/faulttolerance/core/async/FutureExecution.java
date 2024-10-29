package io.smallrye.faulttolerance.core.async;

import static io.smallrye.faulttolerance.core.async.AsyncLogger.LOG;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;
import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;
import static java.util.concurrent.CompletableFuture.failedFuture;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.smallrye.faulttolerance.core.FaultToleranceContext;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.Future;

/**
 * Unlike {@link ThreadOffload}, this is supposed to be the <em>first</em> strategy
 * in the chain. The remaining strategies are executed on an extra thread. This allows using
 * <em>synchronous</em> execution of other strategies to implement {@code Future}-based
 * {@code @Asynchronous} invocations, with two exceptions:
 * <ul>
 * <li>timeouts, where {@code FutureTimeout} is needed in front of {@code Timeout};</li>
 * <li>bulkheads, where {@code FutureBulkhead} is used instead of {@code Bulkhead}.</li>
 * </ul>
 */
public class FutureExecution<V> implements FaultToleranceStrategy<java.util.concurrent.Future<V>> {
    private final FaultToleranceStrategy<java.util.concurrent.Future<V>> delegate;
    private final Executor executor;

    public FutureExecution(FaultToleranceStrategy<java.util.concurrent.Future<V>> delegate, Executor executor) {
        this.delegate = delegate;
        this.executor = checkNotNull(executor, "Executor must be set");
    }

    @Override
    public Future<java.util.concurrent.Future<V>> apply(FaultToleranceContext<java.util.concurrent.Future<V>> ctx) {
        LOG.trace("FutureExecution started");
        try {
            return Future.of(doApply(ctx));
        } finally {
            LOG.trace("FutureExecution finished");
        }
    }

    private java.util.concurrent.Future<V> doApply(FaultToleranceContext<java.util.concurrent.Future<V>> ctx) {
        FutureTask<java.util.concurrent.Future<V>> task = new FutureTask<>(() -> {
            try {
                return delegate.apply(ctx).awaitBlocking();
            } catch (Throwable e) {
                return failedFuture(e);
            }
        });
        executor.execute(task);
        return new java.util.concurrent.Future<>() {
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
