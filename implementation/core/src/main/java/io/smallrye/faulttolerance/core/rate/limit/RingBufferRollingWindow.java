package io.smallrye.faulttolerance.core.rate.limit;

import java.util.Arrays;

import io.smallrye.faulttolerance.core.stopwatch.RunningStopwatch;
import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;

final class RingBufferRollingWindow implements TimeWindow {
    private final RunningStopwatch stopwatch;

    private final long timeWindowInMillis;
    private final long minSpacingInMillis;

    private final long[] timestamps; // length == maxInvocations

    private int head; // index of newest entry
    private int tail; // index of oldest still valid entry

    RingBufferRollingWindow(Stopwatch stopwatch, int maxInvocations, long timeWindowInMillis, long minSpacingInMillis) {
        this.stopwatch = stopwatch.start();
        this.timeWindowInMillis = timeWindowInMillis;
        this.minSpacingInMillis = minSpacingInMillis;
        this.timestamps = new long[maxInvocations];
        Arrays.fill(timestamps, Long.MAX_VALUE);
        this.head = -1;
        this.tail = 0;
    }

    @Override
    public synchronized long record() {
        long now = stopwatch.elapsedTimeInMillis();
        long validity = now - timeWindowInMillis; // all entries before or at this timestamp have expired

        while (timestamps[tail] <= validity && head != tail) {
            advanceTail();
        }

        boolean isFull = isFull();
        long result = !isFull ? 0 : timestamps[tail] - now + timeWindowInMillis;

        if (result == 0 && minSpacingInMillis != 0 && head >= 0) {
            long previous = timestamps[head];
            if (previous != Long.MAX_VALUE) {
                long timeFromPrevious = now - previous;
                if (timeFromPrevious < minSpacingInMillis) {
                    result = minSpacingInMillis - timeFromPrevious;
                }
            }
        }

        if (isFull) {
            advanceTail();
        }
        advanceHead();
        timestamps[head] = now;

        return result;
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
