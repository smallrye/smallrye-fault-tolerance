package io.smallrye.faulttolerance.core.timeout;

import io.smallrye.faulttolerance.core.timer.Timer;
import io.smallrye.faulttolerance.core.timer.TimerTask;

public class TimerTimeoutWatcher implements TimeoutWatcher {
    private final Timer timer;

    public TimerTimeoutWatcher(Timer timer) {
        this.timer = timer;
    }

    @Override
    public TimerTask schedule(TimeoutExecution execution) {
        return timer.schedule(execution.timeoutInMillis(), execution::timeoutAndInterrupt);
    }
}
