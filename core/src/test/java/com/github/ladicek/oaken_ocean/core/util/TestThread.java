package com.github.ladicek.oaken_ocean.core.util;

import java.util.concurrent.Callable;

import static com.github.ladicek.oaken_ocean.core.util.SneakyThrow.sneakyThrow;

public final class TestThread<V> extends Thread {
    private final Callable<V> action;

    private volatile V result;
    private volatile Throwable exception;

    public static <V> TestThread<V> runOnTestThread(Callable<V> action) {
        TestThread<V> thread = new TestThread<>(action);
        thread.start();
        return thread;
    }

    private TestThread(Callable<V> action) {
        this.action = action;
    }

    @Override
    public void run() {
        try {
            result = action.call();
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
