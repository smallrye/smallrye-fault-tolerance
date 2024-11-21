package io.smallrye.faulttolerance.internal;

import java.util.function.Function;

import jakarta.interceptor.InvocationContext;

import io.smallrye.faulttolerance.core.invocation.Invoker;

public class InterceptionInvoker<V> implements Invoker<V> {
    private final InvocationContext invocationContext;

    public InterceptionInvoker(InvocationContext invocationContext) {
        this.invocationContext = invocationContext;
    }

    @Override
    public int parametersCount() {
        return invocationContext.getParameters().length;
    }

    @Override
    public <T> T getArgument(int index, Class<T> parameterType) {
        return parameterType.cast(invocationContext.getParameters()[index]);
    }

    @Override
    public <T> T replaceArgument(int index, Class<T> parameterType, Function<T, T> transformation) {
        Object[] arguments = invocationContext.getParameters();
        T oldArg = parameterType.cast(arguments[index]);
        T newArg = transformation.apply(oldArg);
        arguments[index] = newArg;
        invocationContext.setParameters(arguments);
        return oldArg;
    }

    @Override
    public V proceed() throws Exception {
        return (V) invocationContext.proceed();
    }
}
