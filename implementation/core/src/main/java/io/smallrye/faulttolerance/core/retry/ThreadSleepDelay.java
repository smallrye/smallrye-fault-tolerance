package io.smallrye.faulttolerance.core.retry;

import io.smallrye.faulttolerance.core.util.Preconditions;

public class ThreadSleepDelay implements Delay {
    private final long delayInMillis;
    private final Jitter jitter;

    public ThreadSleepDelay(long delayInMillis, Jitter jitter) {
        this.delayInMillis = Preconditions.check(delayInMillis, delayInMillis >= 0, "Delay must be >= 0");
        this.jitter = Preconditions.checkNotNull(jitter, "Jitter must be set");
    }

    @Override
    public void sleep() throws InterruptedException {
        long sleep = delayInMillis + jitter.generate();
        if (sleep > 0) {
            Thread.sleep(sleep);
        }
    }
}
