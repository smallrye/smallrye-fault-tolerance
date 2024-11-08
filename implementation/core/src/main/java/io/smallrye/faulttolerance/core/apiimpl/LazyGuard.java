package io.smallrye.faulttolerance.core.apiimpl;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import jakarta.enterprise.util.TypeLiteral;

import io.smallrye.faulttolerance.api.Guard;

public final class LazyGuard implements Guard {
    private final Supplier<GuardImpl> builder;

    private final ReentrantLock lock = new ReentrantLock();

    private volatile GuardImpl instance;

    LazyGuard(Supplier<GuardImpl> builder) {
        this.builder = builder;
    }

    @Override
    public <T> T call(Callable<T> action, Class<T> type) throws Exception {
        return instance().call(action, type);
    }

    @Override
    public <T> T call(Callable<T> action, TypeLiteral<T> type) throws Exception {
        return instance().call(action, type);
    }

    @Override
    public <T> T get(Supplier<T> action, Class<T> type) {
        return instance().get(action, type);
    }

    @Override
    public <T> T get(Supplier<T> action, TypeLiteral<T> type) {
        return instance().get(action, type);
    }

    public GuardImpl instance() {
        GuardImpl instance = this.instance;
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
