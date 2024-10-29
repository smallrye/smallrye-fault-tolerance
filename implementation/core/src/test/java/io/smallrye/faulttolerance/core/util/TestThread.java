package io.smallrye.faulttolerance.core.util;

import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;

import io.smallrye.faulttolerance.core.FaultToleranceContext;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;

public final class TestThread<V> extends Thread {
    private final FaultToleranceStrategy<V> strategy;
    private final boolean isAsync;

    private volatile V result;
    private volatile Throwable exception;

    public static <V> TestThread<V> runOnTestThread(FaultToleranceStrategy<V> strategy, boolean isAsync) {
        TestThread<V> thread = new TestThread<>(strategy, isAsync);
        thread.start();
        return thread;
    }

    private TestThread(FaultToleranceStrategy<V> strategy, boolean isAsync) {
        super("TestThread");
        this.strategy = strategy;
        this.isAsync = isAsync;
    }

    @Override
    public void run() {
        try {
            // `TestInvocation` never calls `ctx.call()`, so we can safely pass `null`
            // (`TestInvocation` is used instead of `Invocation` to enable fine-grained testing)
            result = strategy.apply(new FaultToleranceContext<>(null, isAsync)).awaitBlocking();
        } catch (Throwable e) {
            exception = e;
        }
    }

    public V await() throws Exception {
        try {
            this.join();
        } catch (InterruptedException e) {
            // unexpected at this time
            // throw something else, as we want to test that interruption was propagated correctly
            throw new AssertionError("Unexpected interruption", e);
        }

        if (exception != null) {
            throw sneakyThrow(exception);
        }
        return result;
    }

    public boolean isDone() {
        return exception != null || result != null;
    }
}
