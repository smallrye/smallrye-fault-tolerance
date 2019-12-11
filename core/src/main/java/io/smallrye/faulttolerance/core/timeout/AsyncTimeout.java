package io.smallrye.faulttolerance.core.timeout;

import static io.smallrye.faulttolerance.core.timeout.Timeout.timeoutException;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.util.NamedFutureTask;

/**
 * The next strategy in the chain must be {@link Timeout}.
 * They communicate using {@link InvocationContext.Event#TIMEOUT}.
 */
public class AsyncTimeout<V> implements FaultToleranceStrategy<Future<V>> {
    private final Timeout<Future<V>> delegate;
    private final Executor executor;

    public AsyncTimeout(Timeout<Future<V>> delegate, Executor executor) {
        this.delegate = delegate;
        this.executor = checkNotNull(executor, "Executor must be set");
    }

    @Override
    public Future<V> apply(InvocationContext<Future<V>> ctx) throws Exception {
        AsyncTimeoutTask<Future<V>> task = new AsyncTimeoutTask<>(delegate.description, () -> delegate.apply(ctx));
        ctx.registerEventHandler(InvocationContext.Event.TIMEOUT, task::timedOut);
        executor.execute(task);
        try {
            return task.get();
        } catch (ExecutionException e) {
            throw sneakyThrow(e.getCause());
        }
    }

    // TODO
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> Exception sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    // only to expose `setException`, which is `protected` in `FutureTask`
    private static class AsyncTimeoutTask<T> extends NamedFutureTask<T> {
        public AsyncTimeoutTask(String name, Callable<T> callable) {
            super(name, callable);
        }

        public void timedOut() {
            super.setException(timeoutException(name)); // TODO exception message?
        }
    }
}
