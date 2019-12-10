package io.smallrye.faulttolerance.core.async;

import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.util.NamedFutureTask;

// TODO needs test
public class FutureExecution<V> implements FaultToleranceStrategy<Future<V>> {
    private final FaultToleranceStrategy<Future<V>> delegate;
    private final Executor executor;

    public FutureExecution(FaultToleranceStrategy<Future<V>> delegate, Executor executor) {
        this.delegate = delegate;
        this.executor = checkNotNull(executor, "Executor must be set");
    }

    @Override
    public Future<V> apply(InvocationContext<Future<V>> ctx) {
        FutureTask<Future<V>> task = new NamedFutureTask<>("FutureExecution", () -> delegate.apply(ctx));
        executor.execute(task);
        return new Future<V>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                ctx.fireEvent(InvocationContext.Event.CANCEL);
                return task.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return task.isCancelled();
            }

            @Override
            public boolean isDone() {
                return task.isDone();
            }

            @Override
            public V get() throws InterruptedException, ExecutionException {
                return task.get().get();
            }

            @Override
            public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return task.get().get(timeout, unit);
            }
        };
    }
}
