package io.smallrye.faulttolerance.core.stopwatch;

public class SystemStopwatch implements Stopwatch {
    public static final SystemStopwatch INSTANCE = new SystemStopwatch();

    private SystemStopwatch() {
        // avoid instantiation
    }

    @Override
    public RunningStopwatch start() {
        long start = System.nanoTime();

        return () -> {
            long now = System.nanoTime();
            return (now - start) / 1_000_000;
        };
    }
}
