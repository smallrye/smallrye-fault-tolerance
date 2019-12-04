package com.github.ladicek.oaken_ocean.core.bulkhead;

import java.util.concurrent.Callable;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.SimpleInvocationContext;
import com.github.ladicek.oaken_ocean.core.util.barrier.Barrier;

public final class TestInvocation<V> implements FaultToleranceStrategy<V, SimpleInvocationContext<V>> {
    private final Barrier startBarrier;
    private final Barrier delayBarrier;
    private final Callable<V> result;

    public static <V> TestInvocation<V> immediatelyReturning(Callable<V> result) {
        return new TestInvocation<>(null, null, result);
    }

    public static <V> TestInvocation<V> delayed(Barrier delayBarrier, Callable<V> result) {
        return new TestInvocation<>(null, delayBarrier, result);
    }

    public static <V> TestInvocation<V> delayed(Barrier startBarrier, Barrier delayBarrier, Callable<V> result) {
        return new TestInvocation<>(startBarrier, delayBarrier, result);
    }

    private TestInvocation(Barrier startBarrier, Barrier delayBarrier, Callable<V> result) {
        this.startBarrier = startBarrier;
        this.delayBarrier = delayBarrier;
        this.result = result;
    }

    @Override
    public V apply(SimpleInvocationContext<V> target) throws Exception {
        if (startBarrier != null) {
            startBarrier.open();
        }

        if (delayBarrier != null) {
            delayBarrier.await();
        }

        return result.call();
    }
}
