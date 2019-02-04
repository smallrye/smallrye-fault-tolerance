package com.github.ladicek.oaken_ocean.core.retry;

import static com.github.ladicek.oaken_ocean.core.util.Preconditions.check;
import static com.github.ladicek.oaken_ocean.core.util.Preconditions.checkNotNull;

public class ThreadSleepDelay implements Delay {
    private final long delayInMillis;
    private final Jitter jitter;

    public ThreadSleepDelay(long delayInMillis, Jitter jitter) {
        this.delayInMillis = check(delayInMillis, delayInMillis >= 0, "Delay must be >= 0");
        this.jitter = checkNotNull(jitter, "Jitter must be set");
    }

    @Override
    public void sleep() throws InterruptedException {
        long sleep = delayInMillis + jitter.generate();
        if (sleep > 0) {
            Thread.sleep(sleep);
        }
    }
}
