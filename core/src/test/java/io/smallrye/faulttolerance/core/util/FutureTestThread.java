package io.smallrye.faulttolerance.core.util;

import java.util.concurrent.Future;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.FutureInvocationContext;

public final class FutureTestThread<V> extends Thread {

    private final FaultToleranceStrategy<Future<V>, FutureInvocationContext<V>> invocation;

    private volatile Future<V> result;
    private volatile Throwable exception;
    private FutureInvocationContext<V> target;

    public static <V> FutureTestThread<V> runOnTestThread(
            FaultToleranceStrategy<Future<V>, FutureInvocationContext<V>> invocation,
            FutureInvocationContext<V> target) {
        FutureTestThread<V> thread = new FutureTestThread<V>(invocation);
        thread.target = target;
        thread.start();
        return thread;
    }

    private FutureTestThread(FaultToleranceStrategy<Future<V>, FutureInvocationContext<V>> invocation) {
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
            SneakyThrow.sneakyThrow(exception);
        }
        return result;
    }

    public boolean isDone() {
        return result != null || exception != null;
    }
}
