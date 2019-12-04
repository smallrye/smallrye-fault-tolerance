package com.github.ladicek.oaken_ocean.core.timeout;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ScheduledExecutorTimeoutWatcher implements TimeoutWatcher {
    private final ScheduledExecutorService executor;

    public ScheduledExecutorTimeoutWatcher(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public TimeoutWatch schedule(TimeoutExecution execution) {
        ScheduledFuture<?> future = executor.schedule(execution::timeoutAndInterrupt, execution.timeoutInMillis(),
                TimeUnit.MILLISECONDS);
        return new TimeoutWatch() {
            @Override
            public boolean isRunning() {
                return !future.isDone();
            }

            @Override
            public void cancel() {
                future.cancel(true);
            }
        };
    }
}
