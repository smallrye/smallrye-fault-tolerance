package io.smallrye.faulttolerance.core.timeout;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import io.smallrye.faulttolerance.core.timer.Timer;
import io.smallrye.faulttolerance.core.timer.TimerTask;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;

/**
 * Can only be used once; subsequent usages will throw an exception during {@code schedule}.
 */
public final class TestTimer implements Timer {
    private final AtomicBoolean alreadyUsed = new AtomicBoolean(false);

    private final Barrier timerElapsedBarrier;
    private final Barrier timerTaskFinishedBarrier;

    private final AtomicBoolean timerTaskCancelled = new AtomicBoolean(false);

    private volatile Thread executingThread;

    public TestTimer(Barrier timerElapsedBarrier, Barrier timerTaskFinishedBarrier) {
        this.timerElapsedBarrier = timerElapsedBarrier;
        this.timerTaskFinishedBarrier = timerTaskFinishedBarrier;
    }

    boolean timerTaskCancelled() {
        return timerTaskCancelled.get();
    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public TimerTask schedule(long delayInMillis, Runnable task) {
        if (alreadyUsed.compareAndSet(false, true)) {
            executingThread = new Thread(() -> {
                try {
                    timerElapsedBarrier.await();
                    task.run();
                    timerTaskFinishedBarrier.open();
                } catch (InterruptedException e) {
                    // this is expected in case the watched code doesn't timeout (and so the watch is cancelled)
                    // see also the return value of this method
                }
            }, "TestTimer thread");
            executingThread.start();
            return new TimerTask() {
                @Override
                public boolean isDone() {
                    return !executingThread.isAlive();
                }

                @Override
                public boolean cancel() {
                    if (timerTaskCancelled.compareAndSet(false, true)) {
                        executingThread.interrupt();
                        return true;
                    }
                    return false;
                }
            };
        } else {
            throw new IllegalStateException("TestTimer cannot be reused");
        }
    }

    @Override
    public TimerTask schedule(long delayInMillis, Runnable task, Executor executor) {
        // in the test, the `executor` is always `null`
        return schedule(delayInMillis, task);
    }

    @Override
    public int countScheduledTasks() {
        // not used in `Timeout` / `CompletionStageTimeout`
        throw new UnsupportedOperationException();
    }

    public void shutdown() throws InterruptedException {
        if (executingThread != null) {
            executingThread.interrupt();
            executingThread.join();
        }
    }
}
