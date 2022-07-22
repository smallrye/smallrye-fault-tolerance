package io.smallrye.faulttolerance.core.rate.limit;

import java.util.Arrays;

import io.smallrye.faulttolerance.core.clock.Clock;

final class RingBufferRollingWindow implements TimeWindow {
    private final Clock clock;

    private final long timeWindowInMillis;
    private final long minSpacingInMillis;

    private final long[] timestamps; // length == maxInvocations

    private int head; // index of newest entry
    private int tail; // index of oldest still valid entry

    RingBufferRollingWindow(Clock clock, int maxInvocations, long timeWindowInMillis, long minSpacingInMillis) {
        this.clock = clock;
        this.timeWindowInMillis = timeWindowInMillis;
        this.minSpacingInMillis = minSpacingInMillis;
        this.timestamps = new long[maxInvocations];
        Arrays.fill(timestamps, Long.MAX_VALUE);
        this.head = -1;
        this.tail = 0;
    }

    @Override
    public synchronized boolean record() {
        long now = clock.currentTimeInMillis();
        long validity = now - timeWindowInMillis; // all entries before or at this timestamp have expired

        while (timestamps[tail] <= validity && head != tail) {
            advanceTail();
        }

        boolean isFull = isFull();
        boolean allowInvocation = !isFull;

        if (allowInvocation && minSpacingInMillis != 0 && head >= 0) {
            long previous = timestamps[head];
            if (previous != Long.MAX_VALUE && now - previous < minSpacingInMillis) {
                allowInvocation = false;
            }
        }

        if (isFull) {
            advanceTail();
        }
        advanceHead();
        timestamps[head] = now;

        return allowInvocation;
    }

    private boolean isFull() {
        if (head < 0) {
            return false;
        }
        return (head - tail + 1) % timestamps.length == 0;
    }

    private void advanceHead() {
        head = (head + 1) % timestamps.length;
    }

    private void advanceTail() {
        tail = (tail + 1) % timestamps.length;
    }
}
