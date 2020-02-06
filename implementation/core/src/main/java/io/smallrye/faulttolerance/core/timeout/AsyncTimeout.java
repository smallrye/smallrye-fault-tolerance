package io.smallrye.faulttolerance.core.timeout;

import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;
import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.util.NamedFutureTask;

/**
 * The next strategy in the chain must be {@link Timeout}, and it is invoked on an extra thread.
 * Communication then happens using {@link TimeoutEvent}.
 * <p>
 * Note that the {@code TimeoutException} thrown from this strategy might come from two places:
 * the {@code Timeout} strategy throwing, or {@code AsyncTimeoutTask#timedOut} setting an exception
 * as a result of {@code TimeoutEvent}. Both might happen, and whichever happens first gets to decide.
 * That's why we take extra care to make sure that there's only one place where the exception is created.
 */
// the constructor takes `FaultToleranceStrategy` instead of `Timeout` so that it's possible to insert
// `Tracer` in between. When we replace `Tracer` with proper logging, this can be fixed.
public class AsyncTimeout<V> implements FaultToleranceStrategy<Future<V>> {
    private final FaultToleranceStrategy<Future<V>> delegate;
    private final Executor executor;

    public AsyncTimeout(FaultToleranceStrategy<Future<V>> delegate, Executor executor) {
        this.delegate = delegate;
        this.executor = checkNotNull(executor, "Executor must be set");
    }

    @Override
    public Future<V> apply(InvocationContext<Future<V>> ctx) throws Exception {
        AsyncTimeoutTask<Future<V>> task = new AsyncTimeoutTask<>("AsyncTimeout", () -> delegate.apply(ctx));
        ctx.registerEventHandler(TimeoutEvent.class, task::timedOut);
        executor.execute(task);
        try {
            return task.get();
        } catch (ExecutionException e) {
            throw sneakyThrow(e.getCause());
        }
    }

    // only to expose `setException`, which is `protected` in `FutureTask`
    private static class AsyncTimeoutTask<T> extends NamedFutureTask<T> {
        public AsyncTimeoutTask(String name, Callable<T> callable) {
            super(name, callable);
        }

        public void timedOut(TimeoutEvent event) {
            super.setException(event.timeoutException());
        }
    }
}
