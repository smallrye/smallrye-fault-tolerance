package io.smallrye.faulttolerance.core.apiimpl;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import io.smallrye.faulttolerance.api.FaultTolerance;

public final class LazyFaultTolerance<T> implements FaultTolerance<T> {
    private final Supplier<FaultTolerance<T>> builder;
    private final Class<?> asyncType;

    private final ReentrantLock lock = new ReentrantLock();

    private volatile FaultTolerance<T> instance;

    LazyFaultTolerance(Supplier<FaultTolerance<T>> builder, Class<?> asyncType) {
        this.builder = builder;
        this.asyncType = asyncType;
    }

    public Class<?> internalGetAsyncType() {
        return asyncType;
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

    private FaultTolerance<T> instance() {
        FaultTolerance<T> instance = this.instance;
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
