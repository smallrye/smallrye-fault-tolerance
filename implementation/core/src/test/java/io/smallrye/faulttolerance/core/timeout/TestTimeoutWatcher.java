package io.smallrye.faulttolerance.core.timeout;

import java.util.concurrent.atomic.AtomicBoolean;

import io.smallrye.faulttolerance.core.timer.TimerTask;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;

/**
 * Can only be used once; subsequent usages will throw an exception during {@code schedule}.
 */
public final class TestTimeoutWatcher implements TimeoutWatcher {
    private final AtomicBoolean alreadyUsed = new AtomicBoolean(false);

    private final Barrier timeoutElapsedBarrier;
    private final Barrier executionInterruptedBarrier;

    private final AtomicBoolean timeoutWatchCancelled = new AtomicBoolean(false);

    private volatile Thread executingThread;

    public TestTimeoutWatcher(Barrier timeoutElapsedBarrier, Barrier executionInterruptedBarrier) {
        this.timeoutElapsedBarrier = timeoutElapsedBarrier;
        this.executionInterruptedBarrier = executionInterruptedBarrier;
    }

    boolean timeoutWatchWasCancelled() {
        return timeoutWatchCancelled.get();
    }

    @Override
    public TimerTask schedule(TimeoutExecution execution) {
        if (alreadyUsed.compareAndSet(false, true)) {
            executingThread = new Thread(() -> {
                try {
                    timeoutElapsedBarrier.await();
                    execution.timeoutAndInterrupt();
                    executionInterruptedBarrier.open();
                } catch (InterruptedException e) {
                    // this is expected in case the watched code doesn't timeout (and so the watch is cancelled)
                    // see also the return value of this method
                }
            }, "TestTimeoutWatcher thread");
            executingThread.start();
            return new TimerTask() {
                @Override public boolean isDone (){
                    return !executingThread.isAlive();
                }

                @Override
                public boolean cancel() {
                    timeoutWatchCancelled.set(true);
                    executingThread.interrupt();
                    return true;
                }
            };
        } else {
            throw new IllegalStateException("TestTimeoutWatcher cannot be reused");
        }
    }

    public void shutdown() throws InterruptedException {
        if (executingThread != null) {
            executingThread.interrupt();
            executingThread.join();
        }
    }
}
