package io.smallrye.faulttolerance.vertx;

import static io.smallrye.faulttolerance.vertx.VertxLogger.LOG;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.faulttolerance.core.scheduler.Scheduler;
import io.smallrye.faulttolerance.core.scheduler.SchedulerRunnableWrapper;
import io.smallrye.faulttolerance.core.scheduler.SchedulerTask;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

final class VertxScheduler implements Scheduler {
    private final Context vertxContext;
    private final Vertx vertx;

    VertxScheduler(Context vertxContext) {
        this.vertxContext = vertxContext;
        this.vertx = vertxContext.owner();
    }

    @Override
    public SchedulerTask schedule(long delayInMillis, Runnable runnable) {
        Runnable wrappedRunnable = SchedulerRunnableWrapper.INSTANCE.wrap(runnable);

        AtomicInteger taskState = new AtomicInteger(VertxSchedulerTask.STATE_NEW);
        AtomicReference<Long> taskTimerId = new AtomicReference<>();

        VertxSchedulerTask task = new VertxSchedulerTask(vertx, taskState, taskTimerId);

        // need to run `vertx.setTimer` on the correct event loop thread
        // (note that single Vert.x instance can manage multiple event loops!)
        vertxContext.runOnContext(ignored -> {
            long timerId = vertx.setTimer(delayInMillis, ignored2 -> {
                if (taskState.compareAndSet(VertxSchedulerTask.STATE_NEW, VertxSchedulerTask.STATE_RUNNING)) {
                    try {
                        LOG.runningEventLoopTask(task);
                        wrappedRunnable.run();
                    } finally {
                        taskState.set(VertxSchedulerTask.STATE_FINISHED);
                    }
                }
            });
            taskTimerId.set(timerId);
        });
        return task;
    }
}
