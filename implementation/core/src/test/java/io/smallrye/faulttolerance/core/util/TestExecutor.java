package io.smallrye.faulttolerance.core.util;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Can only be used once; subsequent usages will throw an exception during {@code execute}.
 */
public class TestExecutor implements Executor {
    private final AtomicBoolean alreadyUsed = new AtomicBoolean(false);

    private volatile Thread executingThread;

    @Override
    public void execute(Runnable command) {
        if (alreadyUsed.compareAndSet(false, true)) {
            executingThread = new Thread(command, "TestExecutor thread");
            executingThread.start();
        } else {
            throw new IllegalStateException("TestExecutor cannot be reused");
        }
    }

    public void interruptExecutingThread() {
        executingThread.interrupt();
    }

    public void waitUntilDone() throws InterruptedException {
        while (executingThread.isAlive()) {
            Thread.sleep(10);
        }
    }
}
