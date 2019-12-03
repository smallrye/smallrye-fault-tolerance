package io.smallrye.faulttolerance.impl;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import com.github.ladicek.oaken_ocean.core.Cancellator;

import io.smallrye.faulttolerance.FaultToleranceInterceptor;

/**
 * @author Antoine Sabot-Durand
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class AsyncFuture<T> implements Future<T> {

    private static final Logger LOGGER = Logger.getLogger(FaultToleranceInterceptor.class);

    private final Future<?> delegate;
    private final Cancellator cancellator;

    public AsyncFuture(Future<?> delegate, Cancellator cancellator) {
        this.delegate = delegate;
        this.cancellator = cancellator;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        cancellator.cancel(mayInterruptIfRunning);
        return delegate.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return delegate.isCancelled();
    }

    @Override
    public boolean isDone() {
        return delegate.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        Future<T> future;
        try {
            future = unwrapFuture(delegate.get());
        } catch (ExecutionException e) {
            if (isCancellation(e)) {
                throw new CancellationException();
            }
            throw e;
        }
        try {
            return logResult(future, future.get());
        } catch (ExecutionException e) {
            if (isCancellation(e)) {
                throw new CancellationException();
            }
            // Rethrow if completed exceptionally
            throw e;
        }
    }

    private boolean isCancellation(ExecutionException executionException) {
        return executionException.getCause() instanceof InterruptedException;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException {
        Future<T> future;
        try {
            future = unwrapFuture(delegate.get());
        } catch (ExecutionException e) {
            if (isCancellation(e)) {
                throw new CancellationException();
            }
            throw e;
        }
        try {
            return logResult(future, future.get(timeout, unit));
        } catch (ExecutionException e) {
            // Rethrow if completed exceptionally
            throw e;
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Future<T> unwrapFuture(Object futureObject) {
        if (futureObject instanceof Future) {
            return (Future<T>) futureObject;
        } else {
            throw new IllegalStateException("A result of an @Asynchronous call must be Future: " + futureObject);
        }
    }

    private T logResult(Future<T> future, T unwrapped) {
        LOGGER.tracef("Unwrapped async result from %s: %s", future, unwrapped);
        return unwrapped;
    }
}
