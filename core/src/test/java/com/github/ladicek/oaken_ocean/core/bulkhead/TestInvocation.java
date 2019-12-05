package com.github.ladicek.oaken_ocean.core.bulkhead;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.SimpleInvocationContext;
import com.github.ladicek.oaken_ocean.core.util.barrier.Barrier;

public final class TestInvocation<V> implements FaultToleranceStrategy<V, SimpleInvocationContext<V>> {
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

    private TestInvocation(Barrier startBarrier, Barrier delayBarrier, CountDownLatch startedLatch, Callable<V> result) {
        this.startBarrier = startBarrier;
        this.delayBarrier = delayBarrier;
        this.result = result;
        this.startedLatch = startedLatch;
    }

    public static <V> TestInvocation<V> delayed(Barrier startBarrier, Barrier delayBarrier,
            CountDownLatch startedLatch, Callable<V> result) {
        return new TestInvocation<>(startBarrier, delayBarrier, startedLatch, result);
    }

    @Override
    public V apply(SimpleInvocationContext<V> target) throws Exception {
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
