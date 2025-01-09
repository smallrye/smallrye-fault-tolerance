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
            //
            // beware that a Vert.x thread (especially worker) doesn't necessarily have to have a current context set,
            // because a task can be submitted to it outside of a Vert.x context
            Context context = Vertx.currentContext();
            if (context != null) {
                return new VertxExecutor(context, Context.isOnWorkerThread());
            }
        }
        return null;
    }
}
