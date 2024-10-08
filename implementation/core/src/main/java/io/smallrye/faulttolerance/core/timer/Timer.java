package io.smallrye.faulttolerance.core.timer;

import java.util.concurrent.Executor;

/**
 * Timer allows scheduling tasks for execution in the future. Tasks are always executed
 * on a well-defined {@link Executor}. Timer implementations must have a default executor
 * for tasks scheduled using {@link #schedule(long, Runnable)}. Tasks scheduled using
 * {@link #schedule(long, Runnable, Executor)} are executed on the given executor.
 */
public interface Timer {
    /**
     * Returns the ID of this timer. Timer IDs are guaranteed to be unique.
     *
     * @return the ID of this timer
     */
    int getId();

    /**
     * Schedules the {@code task} to be executed in {@code delayInMillis} on this timer's
     * default {@link Executor}.
     * <p>
     * Equivalent to {@code schedule(delayInMillis, task, null)}.
     */
    TimerTask schedule(long delayInMillis, Runnable task);

    /**
     * Schedules the {@code task} to be executed in {@code delayInMillis} on given {@code executor}.
     * If {@code executor} is {@code null}, this timer's default executor is used.
     */
    TimerTask schedule(long delayInMillis, Runnable task, Executor executor);

    /**
     * Returns the number of tasks that are currently scheduled for execution by the timer.
     * Finished tasks and tasks that are already running are not included.
     *
     * @return the number of currently scheduled tasks
     */
    int countScheduledTasks();

    /**
     * Shuts down this timer. Returns after all internal resources of this timer are shut down.
     */
    void shutdown() throws InterruptedException;
}
