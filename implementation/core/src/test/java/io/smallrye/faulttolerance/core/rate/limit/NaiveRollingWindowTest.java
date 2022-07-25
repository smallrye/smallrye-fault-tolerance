package io.smallrye.faulttolerance.core.rate.limit;

import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;

public class NaiveRollingWindowTest extends AbstractRollingWindowTest {
    @Override
    protected TimeWindow createRollingWindow(Stopwatch stopwatch, int maxInvocations, long timeWindowInMillis,
            long minSpacingInMillis) {
        return new NaiveRollingWindow(stopwatch, maxInvocations, timeWindowInMillis, minSpacingInMillis);
    }
}
