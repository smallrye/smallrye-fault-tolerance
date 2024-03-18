package io.smallrye.faulttolerance.core.rate.limit;

import java.util.ArrayList;
import java.util.List;

import io.smallrye.faulttolerance.core.stopwatch.RunningStopwatch;
import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;

final class NaiveRollingWindow implements TimeWindow {
    private final RunningStopwatch stopwatch;

    private final int maxInvocations;
    private final long timeWindowInMillis;
    private final long minSpacingInMillis;

    private final List<Long> timestamps = new ArrayList<>();

    NaiveRollingWindow(Stopwatch stopwatch, int maxInvocations, long timeWindowInMillis, long minSpacingInMillis) {
        this.stopwatch = stopwatch.start();
        this.maxInvocations = maxInvocations;
        this.timeWindowInMillis = timeWindowInMillis;
        this.minSpacingInMillis = minSpacingInMillis;
    }

    @Override
    public synchronized long record() {
        long now = stopwatch.elapsedTimeInMillis();
        long validity = now - timeWindowInMillis; // all entries before or at this timestamp have expired

        timestamps.removeIf(it -> it <= validity);

        long result = timestamps.size() < maxInvocations ? 0 : timestamps.get(0) - now + timeWindowInMillis;

        if (result == 0 && minSpacingInMillis != 0 && !timestamps.isEmpty()) {
            long timeFromPrevious = now - timestamps.get(timestamps.size() - 1);
            if (timeFromPrevious < minSpacingInMillis) {
                result = minSpacingInMillis - timeFromPrevious;
            }
        }

        timestamps.add(now);

        return result;
    }
}
