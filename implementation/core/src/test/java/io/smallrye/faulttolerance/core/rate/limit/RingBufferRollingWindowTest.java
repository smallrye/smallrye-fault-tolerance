package io.smallrye.faulttolerance.core.rate.limit;

import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;

public class RingBufferRollingWindowTest extends AbstractRollingWindowTest {
    @Override
    protected TimeWindow createRollingWindow(Stopwatch stopwatch, int maxInvocations, long timeWindowInMillis,
            long minSpacingInMillis) {
        return new RingBufferRollingWindow(stopwatch, maxInvocations, timeWindowInMillis, minSpacingInMillis);
    }
}
