package io.smallrye.faulttolerance.core.timer;

import static io.smallrye.faulttolerance.core.timer.TimerLogger.LOG;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

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
        this.runnable = checkNotNull(runnable, "Runnable task must be set");
        this.onCancel = checkNotNull(onCancel, "Cancellation callback must be set");
    }

    public boolean isDone() {
        int state = this.state.get();
        return state == STATE_FINISHED || state == STATE_CANCELLED;
    }

    public boolean cancel() {
        // can't cancel if it's already running
        if (state.compareAndSet(STATE_NEW, STATE_CANCELLED)) {
            LOG.cancelledTimerTask(this);
            onCancel.accept(this);
            return true;
        }
        return false;
    }
}
