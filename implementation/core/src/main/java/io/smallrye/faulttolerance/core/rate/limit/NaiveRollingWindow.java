package io.smallrye.faulttolerance.core.rate.limit;

import java.util.ArrayList;
import java.util.List;

import io.smallrye.faulttolerance.core.clock.Clock;

final class NaiveRollingWindow implements TimeWindow {
    private final Clock clock;

    private final int maxInvocations;
    private final long timeWindowInMillis;
    private final long minSpacingInMillis;

    private final List<Long> timestamps = new ArrayList<>();

    NaiveRollingWindow(Clock clock, int maxInvocations, long timeWindowInMillis, long minSpacingInMillis) {
        this.clock = clock;
        this.maxInvocations = maxInvocations;
        this.timeWindowInMillis = timeWindowInMillis;
        this.minSpacingInMillis = minSpacingInMillis;
    }

    @Override
    public synchronized boolean record() {
        long now = clock.currentTimeInMillis();
        long validity = now - timeWindowInMillis; // all entries before or at this timestamp have expired

        timestamps.removeIf(it -> it <= validity);

        boolean allowInvocation = timestamps.size() < maxInvocations;

        if (!timestamps.isEmpty()) {
            long timeFromPrevious = now - timestamps.get(timestamps.size() - 1);
            if (timeFromPrevious < minSpacingInMillis) {
                allowInvocation = false;
            }
        }

        timestamps.add(now);

        return allowInvocation;
    }
}
