package io.smallrye.faulttolerance.core.apiimpl;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import io.smallrye.faulttolerance.api.FaultTolerance;

public final class LazyFaultTolerance<T> implements FaultTolerance<T> {
    private final Supplier<FaultTolerance<T>> builder;
    private final Class<?> asyncType;

    private final AtomicReference<FaultTolerance<T>> instance = new AtomicReference<>(null);

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
        FaultTolerance<T> instance = this.instance.get();
        if (instance == null) {
            instance = builder.get();
            if (this.instance.compareAndSet(null, instance)) {
                return instance;
            }
            instance = this.instance.get();
        }
        return instance;
    }
}
