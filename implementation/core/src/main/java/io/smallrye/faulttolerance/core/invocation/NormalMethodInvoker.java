package io.smallrye.faulttolerance.core.invocation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

import io.smallrye.faulttolerance.core.util.Preconditions;

public class NormalMethodInvoker<V> implements Invoker<V> {
    static final Object[] EMPTY_ARRAY = new Object[0];

    private final Method method;
    private final Object target;
    private final Object[] arguments;

    public NormalMethodInvoker(Method method, Object target, Object[] arguments) {
        Preconditions.checkNotNull(arguments, "Arguments array must be set");
        Preconditions.check(arguments.length, arguments.length == method.getParameterCount(),
                "Argument array length must be " + method.getParameterCount());
        this.method = method;
        this.target = target;
        this.arguments = arguments;
    }

    @Override
    public int parametersCount() {
        return method.getParameterCount();
    }

    @Override
    public <T> T getArgument(int index, Class<T> parameterType) {
        return parameterType.cast(arguments[index]);
    }

    @Override
    public <T> T replaceArgument(int index, Class<T> parameterType, Function<T, T> transformation) {
        T oldArg = parameterType.cast(arguments[index]);
        T newArg = transformation.apply(oldArg);
        arguments[index] = newArg;
        return oldArg;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V proceed() throws InvocationTargetException, IllegalAccessException {
        return (V) method.invoke(target, arguments);
    }
}
