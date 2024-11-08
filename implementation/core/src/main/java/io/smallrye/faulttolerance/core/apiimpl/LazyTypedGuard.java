package io.smallrye.faulttolerance.core.apiimpl;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import io.smallrye.faulttolerance.api.TypedGuard;

public final class LazyTypedGuard<V, T> implements TypedGuard<T> {
    private final Supplier<TypedGuardImpl<V, T>> builder;

    private final ReentrantLock lock = new ReentrantLock();

    private volatile TypedGuardImpl<V, T> instance;

    LazyTypedGuard(Supplier<TypedGuardImpl<V, T>> builder) {
        this.builder = builder;
    }

    @Override
    public T call(Callable<T> action) throws Exception {
        return instance().call(action);
    }

    @Override
    public T get(Supplier<T> action) {
        return instance().get(action);
    }

    public TypedGuardImpl<V, T> instance() {
        TypedGuardImpl<V, T> instance = this.instance;
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
