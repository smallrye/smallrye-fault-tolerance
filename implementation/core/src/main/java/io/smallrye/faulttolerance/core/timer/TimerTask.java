package io.smallrye.faulttolerance.core.timer;

/**
 * Result of scheduling a task on a {@link Timer}. Allows observing whether the task
 * {@linkplain #isDone() is done} and {@linkplain #cancel() cancelling} the task.
 */
public interface TimerTask {
    /**
     * Returns whether this task is done. A task is done if it has already finished or was successfully cancelled.
     *
     * @return whether this task is done
     */
    boolean isDone();

    /**
     * Requests cancellation of this task. Task cannot be cancelled when it's already running.
     *
     * @return whether the task was successfully cancelled
     */
    boolean cancel();
}
