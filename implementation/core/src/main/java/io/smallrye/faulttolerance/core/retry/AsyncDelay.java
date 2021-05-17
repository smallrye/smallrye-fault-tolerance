package io.smallrye.faulttolerance.core.retry;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Performs a delay asynchronously. That is, schedules a task to be executed after the delay,
 * potentially on another thread. If no delay is needed, the task may be executed directly
 * on the calling thread.
 */
public interface AsyncDelay {
    /**
     * Runs the {@code task} after delay on an implementation-defined thread.
     * <p>
     * Equivalent to {@code after(task, null)}.
     */
    default void after(Runnable task) {
        after(task, null);
    }

    /**
     * Runs the {@code task} after delay on given {@link Executor}.
     * If given {@code executor} is {@code null}, the task is executed on an implementation-defined thread.
     */
    void after(Runnable task, Executor executor);

    Supplier<AsyncDelay> NONE = () -> (task, executor) -> task.run();
}
