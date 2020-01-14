package io.smallrye.faulttolerance.core.bulkhead;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;

public final class TestInvocation<V> implements FaultToleranceStrategy<V> {
    private final Barrier startBarrier;
    private final Barrier delayBarrier;
    private final Callable<V> result;
    private final CountDownLatch startedLatch;

    public static <V> TestInvocation<V> immediatelyReturning(Callable<V> result) {
        return new TestInvocation<>(null, null, null, result);
    }

    public static <V> TestInvocation<V> delayed(Barrier delayBarrier, Callable<V> result) {
        return new TestInvocation<>(null, delayBarrier, null, result);
    }

    public static <V> TestInvocation<V> delayed(Barrier startBarrier, Barrier delayBarrier, Callable<V> result) {
        return new TestInvocation<>(startBarrier, delayBarrier, null, result);
    }

    public static <V> TestInvocation<V> delayed(Barrier startBarrier, Barrier delayBarrier,
            CountDownLatch startedLatch, Callable<V> result) {
        return new TestInvocation<>(startBarrier, delayBarrier, startedLatch, result);
    }

    private TestInvocation(Barrier startBarrier, Barrier delayBarrier, CountDownLatch startedLatch, Callable<V> result) {
        this.startBarrier = startBarrier;
        this.delayBarrier = delayBarrier;
        this.result = result;
        this.startedLatch = startedLatch;
    }

    @Override
    public V apply(InvocationContext<V> ctx) throws Exception {
        if (startBarrier != null) {
            startBarrier.open();
        }

        if (startedLatch != null) {
            startedLatch.countDown();
        }

        if (delayBarrier != null) {
            delayBarrier.await();
        }

        return result.call();
    }
}
