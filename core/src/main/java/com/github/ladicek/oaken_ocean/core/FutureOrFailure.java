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
        System.out.println("canceling the future or failure, with interrupting: " + mayInterruptIfRunning); // mstodo remove
        this.canceled = true;
        this.mayInterruptIfRunning = mayInterruptIfRunning;
        if (delegate != null) {
            delegate.cancel(true);
        }
        if (mayInterruptIfRunning) {
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
        System.out.println("canceling");
        // mstodo synchronization
        if (failure == null && (delegate == null || !delegate.isDone())) {
            System.out.println("in cancel");
            setFailure(new TimeoutException()); // mstodo propagate some message?
        }
        latch.countDown();
        System.out.println("releasing");
    }
}