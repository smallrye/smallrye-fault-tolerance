package com.github.ladicek.oaken_ocean.core.timeout;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledExecutorTimeoutWatcher implements TimeoutWatcher {
    private final ScheduledExecutorService executor;

    public ScheduledExecutorTimeoutWatcher(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public void schedule(TimeoutExecution execution) {
        executor.schedule(execution::timeoutAndInterrupt, execution.timeoutInMillis(), TimeUnit.MILLISECONDS);
    }
}
