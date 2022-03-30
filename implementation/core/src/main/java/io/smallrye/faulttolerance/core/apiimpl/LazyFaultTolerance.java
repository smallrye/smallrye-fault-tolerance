package io.smallrye.faulttolerance.core.apiimpl;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import io.smallrye.faulttolerance.api.FaultTolerance;

public final class LazyFaultTolerance<T> implements FaultTolerance<T> {
    private final Supplier<FaultTolerance<T>> builder;
    private final Class<?> asyncType;

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
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    instance = builder.get();
                }
            }
        }

        return instance.call(action);
    }
}
