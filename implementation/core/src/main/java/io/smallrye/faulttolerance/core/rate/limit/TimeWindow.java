package io.smallrye.faulttolerance.core.rate.limit;

import io.smallrye.faulttolerance.core.clock.Clock;

public interface TimeWindow {
    /**
     * Records an invocation attempt.
     *
     * @return whether the invocation should be allowed
     */
    boolean record();

    static TimeWindow createFixed(Clock clock, int maxInvocations, long timeWindowInMillis, long minSpacingInMillis) {
        return new FixedWindow(clock, maxInvocations, timeWindowInMillis, minSpacingInMillis);
    }

    static TimeWindow createRolling(Clock clock, int maxInvocations, long timeWindowInMillis, long minSpacingInMillis) {
        return new RingBufferRollingWindow(clock, maxInvocations, timeWindowInMillis, minSpacingInMillis);
    }
}
