package io.smallrye.faulttolerance.core.apiimpl;

import java.util.concurrent.Callable;
import java.util.function.Function;

import io.smallrye.faulttolerance.core.invocation.Invoker;

final class CallableInvoker<V> implements Invoker<V> {
    private final Callable<V> callable;

    CallableInvoker(Callable<V> callable) {
        this.callable = callable;
    }

    @Override
    public int parametersCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getArgument(int index, Class<T> parameterType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T replaceArgument(int index, Class<T> parameterType, Function<T, T> transformation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V proceed() throws Exception {
        return callable.call();
    }
}
