package io.smallrye.faulttolerance.core.stopwatch;

public interface RunningStopwatch {
    /**
     * Returns the number of milliseconds that elapsed since {@link Stopwatch#start()}.
     */
    long elapsedTimeInMillis();
}
