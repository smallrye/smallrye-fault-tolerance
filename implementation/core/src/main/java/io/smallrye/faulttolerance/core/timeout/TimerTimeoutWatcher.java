package io.smallrye.faulttolerance.core.timeout;

import io.smallrye.faulttolerance.core.scheduler.Timer;
import io.smallrye.faulttolerance.core.scheduler.TimerTask;

public class TimerTimeoutWatcher implements TimeoutWatcher {
    private final Timer timer;

    public TimerTimeoutWatcher(Timer timer) {
        this.timer = timer;
    }

    @Override
    public TimeoutWatch schedule(TimeoutExecution execution) {
        TimerTask task = timer.schedule(execution.timeoutInMillis(), execution::timeoutAndInterrupt);
        return new TimeoutWatch() {
            @Override
            public boolean isRunning() {
                return !task.isDone();
            }

            @Override
            public void cancel() {
                task.cancel();
            }
        };
    }
}
