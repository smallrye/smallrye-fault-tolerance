package io.smallrye.faulttolerance.core.async;

import static io.smallrye.faulttolerance.core.async.AsyncLogger.LOG;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.event.loop.EventLoop;

public class RememberEventLoop<V> implements FaultToleranceStrategy<CompletionStage<V>> {
    private final FaultToleranceStrategy<CompletionStage<V>> delegate;
    private final EventLoop eventLoop;

    public RememberEventLoop(FaultToleranceStrategy<CompletionStage<V>> delegate, EventLoop eventLoop) {
        this.delegate = delegate;
        this.eventLoop = eventLoop;
    }

    @Override
    public CompletionStage<V> apply(InvocationContext<CompletionStage<V>> ctx) throws Exception {
        LOG.trace("RememberEventLoopExecutor started");
        try {
            return doApply(ctx);
        } finally {
            LOG.trace("RememberEventLoopExecutor finished");
        }
    }

    private CompletionStage<V> doApply(InvocationContext<CompletionStage<V>> ctx) throws Exception {
        if (eventLoop.isEventLoopThread()) {
            ctx.set(Executor.class, eventLoop.executor());
        }

        return delegate.apply(ctx);
    }
}
