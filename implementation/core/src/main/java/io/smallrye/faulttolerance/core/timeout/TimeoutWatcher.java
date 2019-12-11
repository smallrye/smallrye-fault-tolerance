package io.smallrye.faulttolerance.core.timeout;

public interface TimeoutWatcher {
    TimeoutWatch schedule(TimeoutExecution execution);
}
