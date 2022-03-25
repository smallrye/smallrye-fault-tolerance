package io.smallrye.faulttolerance.internal;

import java.util.function.Function;

import javax.interceptor.InvocationContext;

import io.smallrye.faulttolerance.core.invocation.Invoker;

public class InterceptionInvoker<V> implements Invoker<V> {
    private final InvocationContext interceptionContext;

    public InterceptionInvoker(InvocationContext interceptionContext) {
        this.interceptionContext = interceptionContext;
    }

    @Override
    public int parametersCount() {
        return interceptionContext.getParameters().length;
    }

    @Override
    public <T> T getArgument(int index, Class<T> parameterType) {
        return parameterType.cast(interceptionContext.getParameters()[index]);
    }

    @Override
    public <T> T replaceArgument(int index, Class<T> parameterType, Function<T, T> transformation) {
        Object[] arguments = interceptionContext.getParameters();
        T oldArg = parameterType.cast(arguments[index]);
        T newArg = transformation.apply(oldArg);
        arguments[index] = newArg;
        interceptionContext.setParameters(arguments);
        return oldArg;
    }

    @Override
    public V proceed() throws Exception {
        return (V) interceptionContext.proceed();
    }
}
