package com.github.ladicek.oaken_ocean.core.util;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.FutureInvocationContext;

import java.util.concurrent.Future;

import static com.github.ladicek.oaken_ocean.core.util.SneakyThrow.sneakyThrow;

public final class CancellableTestThread<V> extends Thread {

    private final FaultToleranceStrategy<Future<V>, FutureInvocationContext<V>> invocation;

    private volatile Future<V> result;
    private volatile Throwable exception;
    private FutureInvocationContext<V> target;

    public static <V> CancellableTestThread<V> runOnTestThread(
          FaultToleranceStrategy<Future<V>, FutureInvocationContext<V>> invocation, FutureInvocationContext<V> target) {
        CancellableTestThread<V> thread = new CancellableTestThread<V>(invocation);
        thread.target = target;
        thread.start();
        return thread;
    }

    private CancellableTestThread(FaultToleranceStrategy<Future<V>, FutureInvocationContext<V>> invocation) {
        this.invocation = invocation;
    }

    public static <V> FutureInvocationContext<V> mockContext() {
        return new FutureInvocationContext<>(null, null);
    }

    @Override
    public void run() {
        try {
            result = invocation.apply(target);
        } catch (Throwable e) {
            exception = e;
        }
    }

    public Future<V> await() throws Exception {
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
