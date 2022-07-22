package io.smallrye.faulttolerance.core.rate.limit;

import io.smallrye.faulttolerance.core.clock.Clock;

public class NaiveRollingWindowTest extends AbstractRollingWindowTest {
    @Override
    protected TimeWindow createRollingWindow(Clock clock, int maxInvocations, long timeWindowInMillis,
            long minSpacingInMillis) {
        return new NaiveRollingWindow(clock, maxInvocations, timeWindowInMillis, minSpacingInMillis);
    }
}
