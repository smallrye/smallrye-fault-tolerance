package com.github.ladicek.oaken_ocean.core.timeout;

import com.github.ladicek.oaken_ocean.core.util.barrier.Barrier;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Can only be used once; subsequent usages will throw an exception during {@code schedule}.
 */
public final class TestTimeoutWatcher implements TimeoutWatcher {
    private final AtomicBoolean alreadyUsed = new AtomicBoolean(false);

    private final Barrier timeoutElapsedBarrier;
    private final Barrier executionInterruptedBarrier;

    public TestTimeoutWatcher(Barrier timeoutElapsedBarrier, Barrier executionInterruptedBarrier) {
        this.timeoutElapsedBarrier = timeoutElapsedBarrier;
        this.executionInterruptedBarrier = executionInterruptedBarrier;
    }

    @Override
    public void schedule(TimeoutExecution execution) {
        if (alreadyUsed.compareAndSet(false, true)) {
            new Thread(() -> {
                try {
                    timeoutElapsedBarrier.await();
                    execution.timeoutAndInterrupt();
                    executionInterruptedBarrier.open();
                } catch (InterruptedException e) {
                    // TODO should scream louder, somehow
                    System.err.println("Unexpected interruption");
                    e.printStackTrace();
                }
            }).start();
        } else {
            throw new IllegalStateException("TestTimeoutWatcher cannot be reused");
        }
    }
}
