package io.smallrye.faulttolerance.core.timeout;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

final class TimeoutExecution {
    private static final int STATE_RUNNING = 0;
    private static final int STATE_FINISHED = 1;
    private static final int STATE_TIMED_OUT = 2;

    private static final VarHandle STATE = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(),
            "state", VarHandle.class, TimeoutExecution.class, int.class);

    private volatile int state;

    // can be null, if no thread shall be interrupted upon timeout
    private final Thread executingThread;
    // can be null, if no action shall be performed upon timeout
    private final Runnable timeoutAction;

    TimeoutExecution(Thread executingThread, Runnable timeoutAction) {
        this.state = STATE_RUNNING;
        this.executingThread = executingThread;
        this.timeoutAction = timeoutAction;
    }

    boolean isRunning() {
        return state == STATE_RUNNING;
    }

    boolean hasFinished() {
        return state == STATE_FINISHED;
    }

    boolean hasTimedOut() {
        return state == STATE_TIMED_OUT;
    }

    void finish(Runnable ifFinished) {
        if (STATE.compareAndSet(this, STATE_RUNNING, STATE_FINISHED)) {
            ifFinished.run();
        }
    }

    void timeoutAndInterrupt() {
        if (STATE.compareAndSet(this, STATE_RUNNING, STATE_TIMED_OUT)) {
            if (executingThread != null) {
                executingThread.interrupt();
            }
            if (timeoutAction != null) {
                timeoutAction.run();
            }
        }
    }
}
