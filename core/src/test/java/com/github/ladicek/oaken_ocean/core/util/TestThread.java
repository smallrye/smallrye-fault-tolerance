package com.github.ladicek.oaken_ocean.core.util;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.SimpleInvocationContext;

import static com.github.ladicek.oaken_ocean.core.util.SneakyThrow.sneakyThrow;

public final class TestThread<V> extends Thread {
    private final FaultToleranceStrategy<V, SimpleInvocationContext<V>> invocation;

    private volatile V result;
    private volatile Throwable exception;

    public static <V> TestThread<V> runOnTestThread(FaultToleranceStrategy<V, SimpleInvocationContext<V>> invocation) {
        TestThread<V> thread = new TestThread<>(invocation);
        thread.start();
        return thread;
    }

    private TestThread(FaultToleranceStrategy<V, SimpleInvocationContext<V>> invocation) {
        this.invocation = invocation;
    }

    @Override
    public void run() {
        try {
            // all `TestInvocation`s ignore the `target` parameter, so we can safely pass `null`
            // (actually `TestInvocation`s are used instead of the `Invocation` to enable fine-grained testing)
            result = invocation.apply(null);
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
            sneakyThrow(exception);
        }
        return result;
    }
}
