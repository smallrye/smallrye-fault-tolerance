package io.smallrye.faulttolerance.core.invocation;

import java.util.function.Function;

public interface Invoker<V> {
    int parametersCount();

    <T> T getArgument(int index, Class<T> parameterType);

    // callers should think about restoring the original argument later, if necessary
    <T> T replaceArgument(int index, Class<T> parameterType, Function<T, T> transformation);

    V proceed() throws Exception;
}
