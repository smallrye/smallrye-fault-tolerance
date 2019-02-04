package com.github.ladicek.oaken_ocean.core.timeout;

import java.util.concurrent.atomic.AtomicInteger;

final class TimeoutExecution {
    private static final int STATE_RUNNING = 0;
    private static final int STATE_FINISHED = 1;
    private static final int STATE_TIMED_OUT = 2;

    private final AtomicInteger state;

    private final Thread executingThread;

    private final long timeoutInMillis;

    TimeoutExecution(Thread executingThread, long timeoutInMillis) {
        this.state = new AtomicInteger(STATE_RUNNING);
        this.executingThread = executingThread;
        this.timeoutInMillis = timeoutInMillis;
    }

    long timeoutInMillis() {
        return timeoutInMillis;
    }

    boolean hasTimedOut() {
        return state.get() == STATE_TIMED_OUT;
    }

    void finish() {
        state.compareAndSet(STATE_RUNNING, STATE_FINISHED);
    }

    void timeoutAndInterrupt() {
        if (state.compareAndSet(STATE_RUNNING, STATE_TIMED_OUT)) {
            executingThread.interrupt();
        }
    }
}
