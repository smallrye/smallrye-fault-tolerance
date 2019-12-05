package io.smallrye.faulttolerance.core.stopwatch;

public class TestStopwatch implements Stopwatch {
    private volatile long currentValue;

    public void setCurrentValue(long currentValue) {
        this.currentValue = currentValue;
    }

    @Override
    public RunningStopwatch start() {
        return new RunningStopwatch() {
            @Override
            public long elapsedTimeInMillis() {
                return currentValue;
            }
        };
    }
}
