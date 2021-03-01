package io.smallrye.faulttolerance.vertx;

import static io.smallrye.faulttolerance.vertx.VertxLogger.LOG;

import java.util.concurrent.atomic.AtomicBoolean;

import io.smallrye.faulttolerance.core.scheduler.SchedulerTask;
import io.vertx.core.Vertx;

final class VertxEventLoopTask implements SchedulerTask {
    private final Vertx vertx;
    private final long timerId;
    private final AtomicBoolean taskDone;

    VertxEventLoopTask(Vertx vertx, long timerId, AtomicBoolean taskDone) {
        this.vertx = vertx;
        this.timerId = timerId;
        this.taskDone = taskDone;
    }

    @Override
    public boolean isDone() {
        return taskDone.get();
    }

    @Override
    public boolean cancel() {
        boolean cancelled = vertx.cancelTimer(timerId);
        if (cancelled) {
            taskDone.set(true);
            LOG.cancelledEventLoopTask(this);
        }
        return cancelled;
    }
}
