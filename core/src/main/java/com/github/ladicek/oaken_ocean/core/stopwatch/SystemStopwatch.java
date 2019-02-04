package com.github.ladicek.oaken_ocean.core.stopwatch;

public class SystemStopwatch implements Stopwatch {
    @Override
    public RunningStopwatch start() {
        long start = System.nanoTime();

        return new RunningStopwatch() {
            @Override
            public long elapsedTimeInMillis() {
                long now = System.nanoTime();
                return (now - start) / 1_000_000;
            }
        };
    }
}
