package io.smallrye.faulttolerance.core.retry;

import io.smallrye.faulttolerance.core.util.Preconditions;

public class ThreadSleepDelay implements SyncDelay {
    private final BackOff backOff;

    public ThreadSleepDelay(BackOff backOff) {
        this.backOff = Preconditions.checkNotNull(backOff, "Back-off must be set");
    }

    @Override
    public void sleep() throws InterruptedException {
        long delay = backOff.getInMillis();
        if (delay > 0) {
            Thread.sleep(delay);
        }
    }
}
