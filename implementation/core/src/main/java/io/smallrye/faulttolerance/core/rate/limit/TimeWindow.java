package io.smallrye.faulttolerance.core.rate.limit;

import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;

public interface TimeWindow {
    /**
     * Records an invocation attempt.
     *
     * @return whether the invocation should be allowed
     */
    boolean record();

    static TimeWindow createFixed(Stopwatch stopwatch, int maxInvocations, long timeWindowInMillis, long minSpacingInMillis) {
        return new FixedWindow(stopwatch, maxInvocations, timeWindowInMillis, minSpacingInMillis);
    }

    static TimeWindow createRolling(Stopwatch stopwatch, int maxInvocations, long timeWindowInMillis, long minSpacingInMillis) {
        return new RingBufferRollingWindow(stopwatch, maxInvocations, timeWindowInMillis, minSpacingInMillis);
    }

    static TimeWindow createSmooth(Stopwatch stopwatch, int maxInvocations, long timeWindowInMillis, long minSpacingInMillis) {
        return new SmoothWindow(stopwatch, maxInvocations, timeWindowInMillis, minSpacingInMillis);
    }
}
