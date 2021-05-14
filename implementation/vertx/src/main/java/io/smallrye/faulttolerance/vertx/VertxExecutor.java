package io.smallrye.faulttolerance.vertx;

import java.util.concurrent.Executor;

import io.smallrye.faulttolerance.core.scheduler.SchedulerRunnableWrapper;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

final class VertxExecutor implements Executor {
    private final Context vertxContext;

    VertxExecutor(Context vertxContext) {
        this.vertxContext = vertxContext;
    }

    @Override
    public void execute(Runnable runnable) {
        // fast path: if we're on the correct event loop thread already,
        // we can run the task directly
        if (Vertx.currentContext() == vertxContext) {
            runnable.run();
            return;
        }

        Runnable wrappedRunnable = SchedulerRunnableWrapper.INSTANCE.wrap(runnable);

        vertxContext.runOnContext(ignored -> {
            wrappedRunnable.run();
        });
    }
}
