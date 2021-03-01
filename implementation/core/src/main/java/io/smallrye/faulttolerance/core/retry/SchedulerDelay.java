package io.smallrye.faulttolerance.core.retry;

import io.smallrye.faulttolerance.core.scheduler.Scheduler;
import io.smallrye.faulttolerance.core.util.Preconditions;

public class SchedulerDelay implements AsyncDelay {
    private final BackOff backOff;
    private final Scheduler scheduler;

    public SchedulerDelay(BackOff backOff, Scheduler scheduler) {
        this.backOff = Preconditions.checkNotNull(backOff, "Back-off must be set");
        this.scheduler = Preconditions.checkNotNull(scheduler, "Scheduler must be set");
    }

    @Override
    public void after(Runnable task) {
        long delay = backOff.getInMillis();
        if (delay > 0) {
            scheduler.schedule(delay, task);
        } else {
            task.run();
        }
    }
}
