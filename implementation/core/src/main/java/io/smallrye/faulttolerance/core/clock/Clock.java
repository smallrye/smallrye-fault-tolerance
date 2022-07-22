package io.smallrye.faulttolerance.core.clock;

public interface Clock {
    /**
     * Returns current time as the number of milliseconds since the Unix epoch.
     * 
     * @see System#currentTimeMillis()
     */
    long currentTimeInMillis();
}
