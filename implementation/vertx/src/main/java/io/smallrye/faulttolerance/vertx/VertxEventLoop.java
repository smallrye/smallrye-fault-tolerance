package io.smallrye.faulttolerance.vertx;

import java.util.concurrent.Executor;

import io.smallrye.faulttolerance.core.event.loop.EventLoop;
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
}
