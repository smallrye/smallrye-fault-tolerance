package io.smallrye.faulttolerance.apiimpl;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.util.TypeLiteral;

import io.smallrye.faulttolerance.api.Guard;

public final class LazyGuard implements Guard {
    private final Function<String, GuardImpl> builder;

    private final ReentrantLock lock = new ReentrantLock();

    private volatile GuardImpl instance;

    LazyGuard(Function<String, GuardImpl> builder) {
        this.builder = builder;
    }

    @Override
    public <T> T call(Callable<T> action, Class<T> type) throws Exception {
        return instance(null).call(action, type);
    }

    @Override
    public <T> T call(Callable<T> action, TypeLiteral<T> type) throws Exception {
        return instance(null).call(action, type);
    }

    @Override
    public <T> T get(Supplier<T> action, Class<T> type) {
        return instance(null).get(action, type);
    }

    @Override
    public <T> T get(Supplier<T> action, TypeLiteral<T> type) {
        return instance(null).get(action, type);
    }

    public GuardImpl instance(String identifier) {
        GuardImpl instance = this.instance;
        if (instance == null) {
            lock.lock();
            try {
                instance = this.instance;
                if (instance == null) {
                    instance = builder.apply(identifier);
                    this.instance = instance;
                }
            } finally {
                lock.unlock();
            }
        }
        return instance;
    }
}
