package io.smallrye.faulttolerance.core.apiimpl;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.core.metrics.MeteredOperationName;

public final class LazyFaultTolerance<T> implements FaultTolerance<T> {
    private final Supplier<FaultToleranceImpl<?, T>> builder;
    private final Class<?> asyncType;

    private final ReentrantLock lock = new ReentrantLock();

    private volatile FaultToleranceImpl<?, T> instance;

    LazyFaultTolerance(Supplier<FaultToleranceImpl<?, T>> builder, Class<?> asyncType) {
        this.builder = builder;
        this.asyncType = asyncType;
    }

    public Class<?> internalGetAsyncType() {
        return asyncType;
    }

    public T call(Callable<T> action, MeteredOperationName meteredOperationName) throws Exception {
        return instance().call(action, meteredOperationName);
    }

    @Override
    public T call(Callable<T> action) throws Exception {
        return instance().call(action);
    }

    @Override
    public T get(Supplier<T> action) {
        return instance().get(action);
    }

    @Override
    public void run(Runnable action) {
        instance().run(action);
    }

    @Override
    public <U> FaultTolerance<U> cast() {
        // TODO breaks laziness
        return instance().cast();
    }

    @Override
    public <U> FaultTolerance<U> castAsync(Class<?> asyncType) {
        // TODO breaks laziness
        return instance().castAsync(asyncType);
    }

    private FaultToleranceImpl<?, T> instance() {
        FaultToleranceImpl<?, T> instance = this.instance;
        if (instance == null) {
            lock.lock();
            try {
                instance = this.instance;
                if (instance == null) {
                    instance = builder.get();
                    this.instance = instance;
                }
            } finally {
                lock.unlock();
            }
        }
        return instance;
    }
}
