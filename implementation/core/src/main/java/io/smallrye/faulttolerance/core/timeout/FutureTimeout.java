package io.smallrye.faulttolerance.core.timeout;

import static io.smallrye.faulttolerance.core.timeout.TimeoutLogger.LOG;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import java.util.concurrent.Executor;

import io.smallrye.faulttolerance.core.Completer;
import io.smallrye.faulttolerance.core.FaultToleranceContext;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.Future;

/**
 * The next strategy in the chain must be {@link Timeout}, and it is invoked on an extra thread.
 * Communication then happens using {@link FutureTimeoutNotification}.
 * <p>
 * Note that the {@code TimeoutException} emitted by this strategy might come from two places:
 * the {@code Timeout} strategy or {@code FutureTimeoutNotification}. Both might happen,
 * and whichever happens first gets to decide.
 */
public class FutureTimeout<V> implements FaultToleranceStrategy<java.util.concurrent.Future<V>> {
    private final FaultToleranceStrategy<java.util.concurrent.Future<V>> delegate;
    private final Executor executor;

    public FutureTimeout(Timeout<java.util.concurrent.Future<V>> delegate, Executor executor) {
        this.delegate = delegate;
        this.executor = checkNotNull(executor, "Executor must be set");
    }

    @Override
    public Future<java.util.concurrent.Future<V>> apply(FaultToleranceContext<java.util.concurrent.Future<V>> ctx) {
        LOG.trace("FutureTimeout started");
        try {
            Completer<java.util.concurrent.Future<V>> completer = Completer.create();
            ctx.set(FutureTimeoutNotification.class, completer::completeWithError);
            executor.execute(() -> {
                try {
                    completer.complete(delegate.apply(ctx).awaitBlocking());
                } catch (Throwable e) {
                    completer.completeWithError(e);
                }
            });

            return completer.future();
        } finally {
            LOG.trace("FutureTimeout finished");
        }
    }
}
