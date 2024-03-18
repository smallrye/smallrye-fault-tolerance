package io.smallrye.faulttolerance.core.rate.limit;

import io.smallrye.faulttolerance.core.stopwatch.RunningStopwatch;
import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;

final class SmoothWindow implements TimeWindow {
    private final RunningStopwatch stopwatch;

    private final int maxInvocations;
    private final long minSpacingInMillis;
    private final double refreshPermitsPerMillis;
    private final double millisToRefreshOnePermit; // could be `long`, but we always use it as `double`

    private double currentPermits;

    private long lastInvocation;
    private long lastPermitRefresh;

    SmoothWindow(Stopwatch stopwatch, int maxInvocations, long timeWindowInMillis, long minSpacingInMillis) {
        this.stopwatch = stopwatch.start();

        this.refreshPermitsPerMillis = (double) maxInvocations / (double) timeWindowInMillis;
        this.millisToRefreshOnePermit = (double) timeWindowInMillis / (double) maxInvocations;
        this.maxInvocations = maxInvocations;
        this.minSpacingInMillis = minSpacingInMillis;

        this.currentPermits = 1.0;
        this.lastInvocation = -minSpacingInMillis;
        this.lastPermitRefresh = 0;
    }

    @Override
    public synchronized long record() {
        long now = stopwatch.elapsedTimeInMillis();

        double permitsToRefresh = (now - lastPermitRefresh) * refreshPermitsPerMillis;
        if (permitsToRefresh > 0.01) {
            currentPermits = Math.min(currentPermits + permitsToRefresh, maxInvocations);
            lastPermitRefresh = now;
        }

        long result = currentPermits >= 1.0 ? 0 : Math.round((1.0 - currentPermits) * millisToRefreshOnePermit);
        if (result == 0 && minSpacingInMillis != 0) {
            long timeFromPrevious = now - lastInvocation;
            if (timeFromPrevious < minSpacingInMillis) {
                result = minSpacingInMillis - timeFromPrevious;
            }
        }

        if (result == 0) {
            currentPermits--;
        }
        lastInvocation = now;

        return result;
    }
}
