package io.smallrye.faulttolerance.core.clock;

public class SystemClock implements Clock {
    public static final SystemClock INSTANCE = new SystemClock();

    private SystemClock() {
        // avoid instantiation
    }

    @Override
    public long currentTimeInMillis() {
        return System.currentTimeMillis();
    }
}
