package io.smallrye.faulttolerance.core.timeout;

import java.util.concurrent.atomic.AtomicInteger;

final class TimeoutExecution {
    private static final int STATE_RUNNING = 0;
    private static final int STATE_FINISHED = 1;
    private static final int STATE_TIMED_OUT = 2;

    private final AtomicInteger state;

    // can be null, if no thread shall be interrupted upon timeout
    private final Thread executingThread;
    // can be null, if no action shall be performed upon timeout
    private final Runnable timeoutAction;

    private final long timeoutInMillis;

    TimeoutExecution(Thread executingThread, long timeoutInMillis) {
        this(executingThread, timeoutInMillis, null);
    }

    TimeoutExecution(Thread executingThread, long timeoutInMillis, Runnable timeoutAction) {
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
            if (executingThread != null) {
                executingThread.interrupt();
            }
            if (timeoutAction != null) {
                timeoutAction.run();
            }
        }
    }
}
