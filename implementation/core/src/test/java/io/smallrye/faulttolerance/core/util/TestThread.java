package io.smallrye.faulttolerance.core.util;

import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;

public final class TestThread<V> extends Thread {
    private final FaultToleranceStrategy<V> invocation;

    private volatile V result;
    private volatile Throwable exception;

    public static <V> TestThread<V> runOnTestThread(FaultToleranceStrategy<V> invocation) {
        TestThread<V> thread = new TestThread<>(invocation);
        thread.start();
        return thread;
    }

    private TestThread(FaultToleranceStrategy<V> invocation) {
        super("TestThread");
        this.invocation = invocation;
    }

    @Override
    public void run() {
        try {
            // `TestInvocation`s never call `ctx.call()`, so we can safely pass `null`
            // (actually `TestInvocation`s are used instead of the `Invocation` to enable fine-grained testing)
            result = invocation.apply(new InvocationContext<>(null));
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
