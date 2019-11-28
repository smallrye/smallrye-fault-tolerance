package com.github.ladicek.oaken_ocean.core.bulkhead;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class FutureOrFailure<V> implements Future<V> {
    // mstodo synchronization
    private Future<V> delegate;
    private CountDownLatch latch = new CountDownLatch(1);
    private boolean canceled;
    private boolean mayInterruptIfRunning;
    private Exception failure;

    public void setDelegate(Future<V> delegate) {
        this.delegate = delegate;
        if (canceled) {
            delegate.cancel(mayInterruptIfRunning);
        }
        latch.countDown();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        this.canceled = true;
        this.mayInterruptIfRunning = mayInterruptIfRunning;
        return delegate == null || delegate.cancel(true);
    }

    @Override
    public boolean isCancelled() {
        return canceled;
    }

    @Override
    public boolean isDone() {
        return delegate != null && delegate.isDone();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        latch.await();
        return delegate.get();
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
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
        if (delegate != null) {
            throw new IllegalStateException("Failure set when the delegate is already set!");
        }
        this.failure = e;
        latch.countDown();
    }

    public Exception getFailure() {
        return failure;
    }
}