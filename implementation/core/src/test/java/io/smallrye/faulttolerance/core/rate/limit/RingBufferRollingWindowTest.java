package io.smallrye.faulttolerance.core.rate.limit;

import io.smallrye.faulttolerance.core.clock.Clock;

public class RingBufferRollingWindowTest extends AbstractRollingWindowTest {
    @Override
    protected TimeWindow createRollingWindow(Clock clock, int maxInvocations, long timeWindowInMillis,
            long minSpacingInMillis) {
        return new RingBufferRollingWindow(clock, maxInvocations, timeWindowInMillis, minSpacingInMillis);
    }
}
