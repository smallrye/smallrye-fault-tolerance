package io.smallrye.faulttolerance.core.timeout;

import io.smallrye.faulttolerance.core.timer.TimerTask;

public interface TimeoutWatcher {
    TimerTask schedule(TimeoutExecution execution);
}
