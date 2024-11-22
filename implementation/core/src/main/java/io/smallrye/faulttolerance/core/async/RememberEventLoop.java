package io.smallrye.faulttolerance.core.async;

import static io.smallrye.faulttolerance.core.async.AsyncLogger.LOG;

import java.util.concurrent.Executor;

import io.smallrye.faulttolerance.core.FaultToleranceContext;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.event.loop.EventLoop;

public class RememberEventLoop<V> implements FaultToleranceStrategy<V> {
    private final FaultToleranceStrategy<V> delegate;
    private final EventLoop eventLoop;
    private final ThreadOffloadEnabled defaultEnabled;

    public RememberEventLoop(FaultToleranceStrategy<V> delegate, EventLoop eventLoop, boolean defaultEnabled) {
        this.delegate = delegate;
        this.eventLoop = eventLoop;
        this.defaultEnabled = new ThreadOffloadEnabled(defaultEnabled);
    }

    @Override
    public Future<V> apply(FaultToleranceContext<V> ctx) {
        // required for `@ApplyGuard`
        if (ctx.get(ThreadOffloadEnabled.class, defaultEnabled).value) {
            return delegate.apply(ctx);
        }

        LOG.trace("RememberEventLoopExecutor started");
        try {
            if (eventLoop.isEventLoopThread()) {
                ctx.set(Executor.class, eventLoop.executor());
            }

            return delegate.apply(ctx);
        } finally {
            LOG.trace("RememberEventLoopExecutor finished");
        }
    }
}
