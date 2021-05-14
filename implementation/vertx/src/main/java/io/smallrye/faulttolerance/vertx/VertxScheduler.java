package io.smallrye.faulttolerance.vertx;

import static io.smallrye.faulttolerance.vertx.VertxLogger.LOG;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.faulttolerance.core.scheduler.Scheduler;
import io.smallrye.faulttolerance.core.scheduler.SchedulerRunnableWrapper;
import io.smallrye.faulttolerance.core.scheduler.SchedulerTask;
import io.vertx.core.Vertx;

final class VertxScheduler implements Scheduler {
    private final Vertx vertx;

    VertxScheduler(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public SchedulerTask schedule(long delayInMillis, Runnable runnable) {
        Runnable wrappedRunnable = SchedulerRunnableWrapper.INSTANCE.wrap(runnable);

        AtomicBoolean taskDone = new AtomicBoolean(false);
        AtomicReference<VertxSchedulerTask> taskRef = new AtomicReference<>();
        long timerId = vertx.setTimer(delayInMillis, ignored -> {
            try {
                LOG.runningEventLoopTask(taskRef.get());
                wrappedRunnable.run();
            } finally {
                taskDone.set(true);
            }
        });
        VertxSchedulerTask task = new VertxSchedulerTask(vertx, timerId, taskDone);
        taskRef.set(task);
        LOG.scheduledEventLoopTask(task, delayInMillis);
        return task;
    }
}
