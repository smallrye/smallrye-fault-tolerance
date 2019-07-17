package com.github.ladicek.oaken_ocean.core.timeout;

public interface TimeoutWatcher {
    TimeoutWatch schedule(TimeoutExecution execution);
}
