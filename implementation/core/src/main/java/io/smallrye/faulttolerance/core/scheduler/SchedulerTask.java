package io.smallrye.faulttolerance.core.scheduler;

public interface SchedulerTask {
    /**
     * Whether the task has already finished, or was cancelled.
     */
    boolean isDone();

    /**
     * Cancels the task if possible, and returns whether cancellation was successful.
     * Cancellation may be unsuccessful for a variety of reasons:
     * <ul>
     * <li>the task has already finished;</li>
     * <li>the task was already cancelled;</li>
     * <li>the task is running and the scheduler can't cancel running tasks;</li>
     * <li>etc.</li>
     * </ul>
     */
    boolean cancel();
}
