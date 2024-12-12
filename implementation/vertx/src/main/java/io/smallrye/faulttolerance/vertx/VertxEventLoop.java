package io.smallrye.faulttolerance.vertx;

import java.util.concurrent.Executor;

import io.smallrye.faulttolerance.core.event.loop.EventLoop;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

public final class VertxEventLoop implements EventLoop {
    @Override
    public Executor executor() {
        if (Context.isOnVertxThread()) {
            // all Vert.x threads are "event loops", even worker threads
            return new VertxExecutor(Vertx.currentContext(), Context.isOnWorkerThread());
        }
        return null;
    }
}
