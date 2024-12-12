package io.smallrye.faulttolerance.vertx;

import java.util.concurrent.Executor;

import io.smallrye.faulttolerance.core.util.RunnableWrapper;
import io.vertx.core.Context;

final class VertxExecutor implements Executor {
    private final Context vertxContext;
    private final boolean offloadToWorkerThread;

    VertxExecutor(Context vertxContext, boolean offloadToWorkerThread) {
        this.vertxContext = vertxContext;
        this.offloadToWorkerThread = offloadToWorkerThread;
    }

    @Override
    public void execute(Runnable runnable) {
        Runnable wrappedRunnable = RunnableWrapper.INSTANCE.wrap(runnable);

        if (vertxContext.isEventLoopContext() && offloadToWorkerThread) {
            vertxContext.executeBlocking(() -> {
                wrappedRunnable.run();
                return null;
            });
        } else {
            vertxContext.runOnContext(ignored -> {
                wrappedRunnable.run();
            });
        }
    }
}
