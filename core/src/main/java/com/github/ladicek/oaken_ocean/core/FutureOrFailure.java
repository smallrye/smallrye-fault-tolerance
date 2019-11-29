package com.github.ladicek.oaken_ocean.core;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class FutureOrFailure<V> implements Future<V> {
    private final CountDownLatch latch = new CountDownLatch(1);

    // mstodo instead of a ton of volatile fields, it might be better to have
    // mstodo one immutable value holder in an AtomicReference
    private volatile boolean canceled;
    private volatile Future<V> delegate;
    private volatile Exception failure;
    private volatile boolean mayInterruptIfRunning;

    public void setDelegate(Future<V> delegate) {
        this.delegate = delegate;
        if (canceled) {
            // mstodo is this needed/correct?
            delegate.cancel(mayInterruptIfRunning);
        }
        latch.countDown();
    }

    @Override
    public boolean cancel(boolean interrupt) {
        mayInterruptIfRunning = interrupt;
        canceled = true;

        if (delegate != null) {
            delegate.cancel(true);
        }
        if (interrupt) {
            Thread.currentThread().interrupt();
        }
        return true;
    }

    @Override
    public boolean isCancelled() {
        return canceled;
    }

    @Override
    public boolean isDone() {
        return latch.getCount() > 0;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        latch.await();
        if (failure != null) {
            throw new ExecutionException(failure);
        }
        return delegate.get();
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {
        latch.await(timeout, unit);
        if (failure != null) {
            throw new ExecutionException(failure);
        }
        return delegate.get(timeout, unit);
    }

    public Future<V> waitForFutureInitialization() throws Exception {
        latch.await();
        if (failure != null) {
            throw failure;
        }
        return delegate;
    }

    public void setFailure(Exception e) {
        this.failure = e;
        latch.countDown();
    }

    public void timeout() {
        if (failure == null && (delegate == null || !delegate.isDone())) {
            setFailure(new TimeoutException()); // mstodo propagate some message?
        }
        latch.countDown();
    }
}