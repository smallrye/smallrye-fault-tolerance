package io.smallrye.faulttolerance.core.scheduler;

public interface Scheduler {
    /**
     * Schedules the {@code runnable} to be executed in the future, in roughly {@code delayInMillis} millis.
     * Scheduling precision may vary across implementations.
     */
    SchedulerTask schedule(long delayInMillis, Runnable runnable);
}
