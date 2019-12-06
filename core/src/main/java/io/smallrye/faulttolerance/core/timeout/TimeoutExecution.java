package io.smallrye.faulttolerance.core.timeout;

import java.util.concurrent.atomic.AtomicInteger;

final class TimeoutExecution {
    private static final int STATE_RUNNING = 0;
    private static final int STATE_FINISHED = 1;
    private static final int STATE_TIMED_OUT = 2;

    private final AtomicInteger state;

    private final Thread executingThread;
    private final Runnable timeoutAction;

    private final long timeoutInMillis;

    TimeoutExecution(Thread executingThread, long timeoutInMillis) {
        this(executingThread, null, timeoutInMillis);
    }

    @SuppressWarnings("UnnecessaryThis")
    TimeoutExecution(Thread executingThread, Runnable timeoutAction, long timeoutInMillis) {
        this.state = new AtomicInteger(STATE_RUNNING);
        this.executingThread = executingThread;
        this.timeoutInMillis = timeoutInMillis;
        this.timeoutAction = timeoutAction;
    }

    long timeoutInMillis() {
        return timeoutInMillis;
    }

    boolean isRunning() {
        return state.get() == STATE_RUNNING;
    }

    boolean hasFinished() {
        return state.get() == STATE_FINISHED;
    }

    boolean hasTimedOut() {
        return state.get() == STATE_TIMED_OUT;
    }

    void finish(Runnable ifFinished) {
        if (state.compareAndSet(STATE_RUNNING, STATE_FINISHED)) {
            ifFinished.run();
        }
    }

    void timeoutAndInterrupt() {
        if (state.compareAndSet(STATE_RUNNING, STATE_TIMED_OUT)) {
            executingThread.interrupt();
            if (timeoutAction != null) {
                timeoutAction.run();
            }
        }
    }
}
