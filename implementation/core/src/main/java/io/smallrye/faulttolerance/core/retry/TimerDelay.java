package io.smallrye.faulttolerance.core.retry;

import io.smallrye.faulttolerance.core.timer.Timer;
import io.smallrye.faulttolerance.core.util.Preconditions;

public class TimerDelay implements AsyncDelay {
    private final BackOff backOff;
    private final Timer timer;

    public TimerDelay(BackOff backOff, Timer timer) {
        this.backOff = Preconditions.checkNotNull(backOff, "Back-off must be set");
        this.timer = Preconditions.checkNotNull(timer, "Timer must be set");
    }

    @Override
    public void after(Runnable task) {
        long delay = backOff.getInMillis();
        if (delay > 0) {
            timer.schedule(delay, task);
        } else {
            task.run();
        }
    }
}
