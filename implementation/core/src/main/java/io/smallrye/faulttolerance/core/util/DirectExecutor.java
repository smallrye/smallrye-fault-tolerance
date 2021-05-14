package io.smallrye.faulttolerance.core.util;

import java.util.concurrent.Executor;

/**
 * An {@link Executor} that runs tasks directly on the caller thread.
 * Any exception thrown by the task is propagated back to the caller.
 */
public final class DirectExecutor implements Executor {
    public static final DirectExecutor INSTANCE = new DirectExecutor();

    private DirectExecutor() {
    }

    @Override
    public void execute(Runnable runnable) {
        runnable.run();
    }
}
