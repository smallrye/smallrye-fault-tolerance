package io.smallrye.faulttolerance.vertx;

import static io.smallrye.faulttolerance.vertx.VertxLogger.LOG;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.faulttolerance.core.scheduler.EventLoop;
import io.smallrye.faulttolerance.core.scheduler.SchedulerRunnableWrapper;
import io.smallrye.faulttolerance.core.scheduler.SchedulerTask;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

public final class VertxEventLoop implements EventLoop {
    @Override
    public boolean isEventLoopThread() {
        return Context.isOnEventLoopThread();
    }

    @Override
    public SchedulerTask schedule(long delayInMillis, Runnable runnable) {
        Runnable wrappedRunnable = SchedulerRunnableWrapper.INSTANCE.wrap(runnable);

        Vertx vertx = Vertx.currentContext().owner();
        AtomicBoolean taskDone = new AtomicBoolean(false);
        AtomicReference<VertxEventLoopTask> taskRef = new AtomicReference<>();
        long timerId = vertx.setTimer(delayInMillis, ignored -> {
            try {
                LOG.runningEventLoopTask(taskRef.get());
                wrappedRunnable.run();
            } finally {
                taskDone.set(true);
            }
        });
        VertxEventLoopTask task = new VertxEventLoopTask(vertx, timerId, taskDone);
        taskRef.set(task);
        LOG.scheduledEventLoopTask(task, delayInMillis);
        return task;
    }
}
