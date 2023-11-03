package io.smallrye.faulttolerance.standalone;

import io.smallrye.faulttolerance.core.timer.Timer;

final class TimerAccessImpl implements TimerAccess {
    private final Timer timer;

    TimerAccessImpl(Timer timer) {
        this.timer = timer;
    }

    @Override
    public int countScheduledTasks() {
        return timer.countScheduledTasks();
    }
}
