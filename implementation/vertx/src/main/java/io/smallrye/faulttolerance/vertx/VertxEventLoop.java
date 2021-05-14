package io.smallrye.faulttolerance.vertx;

import java.util.concurrent.Executor;

import io.smallrye.faulttolerance.core.scheduler.EventLoop;
import io.smallrye.faulttolerance.core.scheduler.Scheduler;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

public final class VertxEventLoop implements EventLoop {
    @Override
    public boolean isEventLoopThread() {
        return Context.isOnEventLoopThread();
    }

    private void checkEventLoopThread() {
        if (!isEventLoopThread()) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Executor executor() {
        checkEventLoopThread();
        return new VertxExecutor(Vertx.currentContext());
    }

    @Override
    public Scheduler scheduler() {
        checkEventLoopThread();
        return new VertxScheduler(Vertx.currentContext().owner());
    }
}
