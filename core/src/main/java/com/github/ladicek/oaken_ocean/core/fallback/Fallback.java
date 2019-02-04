package com.github.ladicek.oaken_ocean.core.fallback;

import java.util.concurrent.Callable;

public class Fallback<V> implements Callable<V> {
    private final Callable<V> delegate;
    private final String description;

    private final Callable<V> fallback;

    public Fallback(Callable<V> delegate, String description, Callable<V> fallback) {
        this.delegate = delegate;
        this.description = description;
        this.fallback = fallback;
    }

    @Override
    public V call() throws Exception {
        try {
            return delegate.call();
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }

        try {
            return fallback.call();
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
