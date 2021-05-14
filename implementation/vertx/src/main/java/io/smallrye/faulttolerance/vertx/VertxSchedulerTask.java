package io.smallrye.faulttolerance.vertx;

import static io.smallrye.faulttolerance.vertx.VertxLogger.LOG;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.faulttolerance.core.scheduler.SchedulerTask;
import io.vertx.core.Vertx;

final class VertxSchedulerTask implements SchedulerTask {
    static final int STATE_NEW = 0; // was scheduled, but isn't running yet
    static final int STATE_RUNNING = 1; // running on the event loop
    static final int STATE_FINISHED = 2; // finished running
    static final int STATE_CANCELLED = 3; // cancelled before it could be executed

    private final Vertx vertx;
    private final AtomicInteger state;
    private final AtomicReference<Long> timerId;

    VertxSchedulerTask(Vertx vertx, AtomicInteger state, AtomicReference<Long> timerId) {
        this.vertx = vertx;
        this.state = state;
        this.timerId = timerId;
    }

    @Override
    public boolean isDone() {
        int state = this.state.get();
        return state == STATE_FINISHED || state == STATE_CANCELLED;
    }

    @Override
    public boolean cancel() {
        if (state.compareAndSet(STATE_NEW, STATE_CANCELLED)) {
            Long timerId = this.timerId.get();
            boolean cancelled = timerId == null || vertx.cancelTimer(timerId);
            if (cancelled) {
                LOG.cancelledEventLoopTask(this);
            }
            return cancelled;
        }

        return false;
    }
}
