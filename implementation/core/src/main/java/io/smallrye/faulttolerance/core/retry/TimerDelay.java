package io.smallrye.faulttolerance.core.retry;

import java.util.concurrent.Executor;

import io.smallrye.faulttolerance.core.timer.Timer;
import io.smallrye.faulttolerance.core.util.Preconditions;

/**
 * Asynchronous delay based on {@link Timer}. Its default executor is the timer's {@link Executor}.
 */
public class TimerDelay implements AsyncDelay {
    private final BackOff backOff;
    private final Timer timer;

    public TimerDelay(BackOff backOff, Timer timer) {
        this.backOff = Preconditions.checkNotNull(backOff, "Back-off must be set");
        this.timer = Preconditions.checkNotNull(timer, "Timer must be set");
    }

    @Override
    public void after(Throwable cause, Runnable task, Executor executor) {
        long delay = backOff.getInMillis(cause);
        if (delay > 0) {
            timer.schedule(delay, task, executor);
        } else {
            task.run();
        }
    }
}
