package com.github.ladicek.oaken_ocean.core.fallback;

import java.util.concurrent.Callable;

public class Fallback<V> implements Callable<V> {
    private final Callable<V> delegate;
    private final String description;

    private final FallbackFunction<V> fallback;

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
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            failure = e;
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }

        try {
            return fallback.call(failure);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            throw e;
        }
    }
}
