package io.smallrye.faulttolerance.core.rate.limit;

import io.smallrye.faulttolerance.core.stopwatch.RunningStopwatch;
import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;

final class SmoothWindow implements TimeWindow {
    private final RunningStopwatch stopwatch;

    private final int maxInvocations;
    private final long minSpacingInMillis;
    private final double refreshPermitsPerMillis;

    private double currentPermits;

    private long lastInvocation;
    private long lastPermitRefresh;

    SmoothWindow(Stopwatch stopwatch, int maxInvocations, long timeWindowInMillis, long minSpacingInMillis) {
        this.stopwatch = stopwatch.start();

        this.refreshPermitsPerMillis = (double) maxInvocations / (double) timeWindowInMillis;
        this.maxInvocations = maxInvocations;
        this.minSpacingInMillis = minSpacingInMillis;

        this.currentPermits = 1;
        this.lastInvocation = -minSpacingInMillis;
        this.lastPermitRefresh = 0;
    }

    @Override
    public synchronized boolean record() {
        long now = stopwatch.elapsedTimeInMillis();

        double permitsToRefresh = (now - lastPermitRefresh) * refreshPermitsPerMillis;
        if (permitsToRefresh > 0.01) {
            currentPermits = Math.min(currentPermits + permitsToRefresh, maxInvocations);
            lastPermitRefresh = now;
        }

        boolean allowInvocation = currentPermits >= 1.0;
        if (allowInvocation && minSpacingInMillis != 0 && now - lastInvocation < minSpacingInMillis) {
            allowInvocation = false;
        }

        if (allowInvocation) {
            currentPermits--;
        }
        lastInvocation = now;

        return allowInvocation;
    }
}
