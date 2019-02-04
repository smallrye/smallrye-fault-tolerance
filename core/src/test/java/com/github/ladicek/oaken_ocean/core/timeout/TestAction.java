package com.github.ladicek.oaken_ocean.core.timeout;

import com.github.ladicek.oaken_ocean.core.util.barrier.Barrier;

import java.util.concurrent.Callable;

public final class TestAction<V> implements Callable<V> {
    private final Barrier startBarrier;
    private final Barrier delayBarrier;
    private final Callable<V> result;

    public static <V> TestAction<V> immediatelyReturning(Callable<V> result) {
        return new TestAction<>(null, null, result);
    }

    public static <V> TestAction<V> delayed(Barrier delayBarrier, Callable<V> result) {
        return new TestAction<>(null, delayBarrier, result);
    }

    public static <V> TestAction<V> delayed(Barrier startBarrier, Barrier delayBarrier, Callable<V> result) {
        return new TestAction<>(startBarrier, delayBarrier, result);
    }

    private TestAction(Barrier startBarrier, Barrier delayBarrier, Callable<V> result) {
        this.startBarrier = startBarrier;
        this.delayBarrier = delayBarrier;
        this.result = result;
    }

    @Override
    public V call() throws Exception {
        if (startBarrier != null) {
            startBarrier.open();
        }

        if (delayBarrier != null) {
            delayBarrier.await();
        }

        return result.call();
    }
}
