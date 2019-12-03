package com.github.ladicek.oaken_ocean.core;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class FutureOrFailure<V> implements Future<V> {
    private final State<V> EMPTY = new State<>(null, null);

    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicReference<State<V>> currentState = new AtomicReference<>(EMPTY);

    private volatile boolean canceled;
    private volatile boolean mayInterruptIfRunning;

    public void setDelegate(Future<V> delegate) {
        State<V> state = new State<>(delegate, null);
        currentState.compareAndSet(EMPTY, state);
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

        State<V> state = currentState.get();
        if (state.delegate != null) {
            state.delegate.cancel(interrupt);
        }
        if (interrupt) {
            Thread.currentThread().interrupt();
        }

        State<V> canceledState = new State<>(null, new InterruptedException());
        if (currentState.compareAndSet(EMPTY, canceledState)) {
            latch.countDown();
        }

        return true;
    }

    @Override
    public boolean isCancelled() {
        Future<V> delegate = currentState.get().delegate;
        return canceled || (delegate != null && delegate.isCancelled());
    }

    @Override
    public boolean isDone() {
        return latch.getCount() == 0;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        latch.await();
        State<V> state = currentState.get();
        if (state.failure != null) {
            throw new ExecutionException(state.failure);
        }
        return state.delegate.get();
    }

    @Override
    public V get(long timeout, TimeUnit unit)
          throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {
        latch.await(timeout, unit);
        State<V> state = currentState.get();
        if (state.failure != null) {
            throw new ExecutionException(state.failure);
        }
        return state.delegate.get(timeout, unit);
    }

    public Future<V> waitForFutureInitialization() throws Exception {
        latch.await();
        State<V> state = currentState.get();
        if (state.failure != null) {
            throw state.failure;
        }
        return state.delegate;
    }

    public void setFailure(Exception e) {
        currentState.compareAndSet(EMPTY, new State<>(null, e));
        latch.countDown();
    }

    public void timeout(TimeoutException exception) {
        // TODO: simplify?
        State<V> previous = EMPTY;
        State<V> timedOutState = new State<>(null, exception);
        int safetyValve = 0;
        while (!currentState.compareAndSet(previous, timedOutState)) {
            previous = currentState.get();
            if (previous.failure != null
                  || (previous.delegate != null && previous.delegate.isDone())) {
                break;
            }
            if (safetyValve++ > 10) {
                // mstodo: log problem
                break;
            }
        }
        latch.countDown();
    }

    private static class State<V> {
        private final Future<V> delegate;
        private final Exception failure;

        private State(Future<V> delegate, Exception failure) {
            this.delegate = delegate;
            this.failure = failure;
        }
    }
}