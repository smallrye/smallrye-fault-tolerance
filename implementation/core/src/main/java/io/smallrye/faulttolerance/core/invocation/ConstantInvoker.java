package io.smallrye.faulttolerance.core.invocation;

import java.util.function.Function;

public class ConstantInvoker<V> implements Invoker<V> {
    private final V value;

    public static <V> Invoker<V> of(V value) {
        return new ConstantInvoker<>(value);
    }

    private ConstantInvoker(V value) {
        this.value = value;
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
    public V proceed() {
        return value;
    }
}
