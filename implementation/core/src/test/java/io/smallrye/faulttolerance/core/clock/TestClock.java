package io.smallrye.faulttolerance.core.clock;

import java.util.concurrent.ThreadLocalRandom;

public class TestClock implements Clock {
    private volatile long currentValue = ThreadLocalRandom.current().nextLong();

    public synchronized void step(long increment) {
        currentValue += increment;
    }

    @Override
    public long currentTimeInMillis() {
        return currentValue;
    }
}
