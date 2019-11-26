package com.github.ladicek.oaken_ocean.core.fallback;

import java.util.concurrent.Callable;

public class Fallback<V> implements Callable<V> {
    final Callable<V> delegate;
    final String description;

    final FallbackFunction<V> fallback;

    public Fallback(Callable<V> delegate, String description, FallbackFunction<V> fallback) {
        this.delegate = delegate;
        this.description = description;
        this.fallback = fallback;
    }

    @Override
    public V call() throws Exception {
        Throwable failure;
        try {
            return delegate.call();
        } catch (Exception e) {
            failure = e;
        }

        if (failure instanceof InterruptedException || Thread.interrupted()) {
            throw new InterruptedException();
        }

        return fallback.call(failure);
    }
}
