package com.github.ladicek.oaken_ocean.core.retry;

import static com.github.ladicek.oaken_ocean.core.util.SneakyThrow.sneakyThrow;

import java.util.function.Supplier;

import com.github.ladicek.oaken_ocean.core.util.barrier.Barrier;

public class TestDelay implements Delay {
    private final Barrier startBarrier;
    private final Barrier endBarrier;
    private final boolean selfInterrupt;
    private final Supplier<? extends Throwable> exception;

    public static TestDelay normal(Barrier startBarrier, Barrier endBarrier) {
        return new TestDelay(startBarrier, endBarrier, false, null);
    }

    public static TestDelay selfInterrupting(Barrier startBarrier, Barrier endBarrier) {
        return new TestDelay(startBarrier, endBarrier, true, null);
    }

    public static TestDelay exceptionThrowing(Barrier startBarrier, Barrier endBarrier,
            Supplier<? extends Throwable> exception) {
        return new TestDelay(startBarrier, endBarrier, false, exception);
    }

    private TestDelay(Barrier startBarrier, Barrier endBarrier, boolean selfInterrupt,
            Supplier<? extends Throwable> exception) {
        this.startBarrier = startBarrier;
        this.endBarrier = endBarrier;
        this.selfInterrupt = selfInterrupt;
        this.exception = exception;
    }

    @Override
    public void sleep() throws InterruptedException {
        startBarrier.open();
        endBarrier.await();

        if (selfInterrupt) {
            Thread.currentThread().interrupt();
            throw new RuntimeException();
        }

        if (exception != null) {
            sneakyThrow(exception.get());
        }
    }
}
