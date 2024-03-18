package io.smallrye.faulttolerance.core.rate.limit;

import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;

public interface TimeWindow {
    /**
     * Records an invocation attempt.
     * A result of zero means that the invocation should be permitted.
     * A positive result means that the invocation should be rejected
     * and the returned number is the minimum number of milliseconds
     * after which retrying makes sense. A negative result means that
     * the invocation should be rejected, but the "retry after" information
     * is unknown.
     *
     * @return zero when the invocation should be permitted, positive
     *         or negative when the invocation should be rejected
     */
    long record();

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
