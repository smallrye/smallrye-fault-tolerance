package io.smallrye.faulttolerance.core.timer;

import static io.smallrye.faulttolerance.core.timer.TimerLogger.LOG;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class TimerTask {
    static final int STATE_NEW = 0; // was scheduled, but isn't running yet
    static final int STATE_RUNNING = 1; // running on the executor
    static final int STATE_FINISHED = 2; // finished running
    static final int STATE_CANCELLED = 3; // cancelled before it could be executed

    final long startTime; // in nanos, to be compared with System.nanoTime()
    final Runnable runnable;
    final AtomicInteger state = new AtomicInteger(STATE_NEW);

    private final Consumer<TimerTask> onCancel;

    TimerTask(long startTime, Runnable runnable, Consumer<TimerTask> onCancel) {
        this.startTime = startTime;
        this.runnable = runnable;
        this.onCancel = onCancel;
    }

    public boolean isDone() {
        int state = this.state.get();
        return state == STATE_FINISHED || state == STATE_CANCELLED;
    }

    // can't cancel if it's already running
    public void cancel() {
        if (state.compareAndSet(STATE_NEW, STATE_CANCELLED)) {
            LOG.cancelledTask(this);
            onCancel.accept(this);
        }
    }
}
