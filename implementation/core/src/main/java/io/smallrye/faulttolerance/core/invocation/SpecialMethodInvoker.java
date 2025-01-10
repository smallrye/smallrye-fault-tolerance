package io.smallrye.faulttolerance.core.invocation;

import static io.smallrye.faulttolerance.core.util.Preconditions.check;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;
import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.function.Function;

public class SpecialMethodInvoker<V> implements Invoker<V> {
    private final MethodHandle methodHandle;
    private final Object target;
    private final Object[] arguments;

    public SpecialMethodInvoker(Method method, Object target, Object[] arguments) throws ReflectiveOperationException {
        checkNotNull(arguments, "Arguments array must be set");
        check(arguments.length, arguments.length == method.getParameterCount(),
                "Argument array length must be " + method.getParameterCount());

        MethodType methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
        Class<?> declaringClass = method.getDeclaringClass();
        this.methodHandle = MethodHandles.lookup()
                .findSpecial(declaringClass, method.getName(), methodType, declaringClass);
        this.target = target;
        this.arguments = arguments;
    }

    @Override
    public int parametersCount() {
        return arguments.length;
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
    public V proceed() {
        try {
            return (V) methodHandle
                    .bindTo(target)
                    .invokeWithArguments(arguments);
        } catch (Throwable e) {
            throw sneakyThrow(e);
        }
    }
}
