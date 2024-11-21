package io.smallrye.faulttolerance.internal;

import java.lang.reflect.Method;
import java.util.Arrays;

import jakarta.interceptor.InvocationContext;

import io.smallrye.faulttolerance.core.FailureContext;
import io.smallrye.faulttolerance.core.invocation.Invoker;
import io.smallrye.faulttolerance.core.invocation.NormalMethodInvoker;
import io.smallrye.faulttolerance.core.invocation.SpecialMethodInvoker;

public final class FallbackMethod {
    private static final Object[] EMPTY_ARRAY = {};

    private final int exceptionParameterPosition; // < 0 if no exception parameter exists
    private final Method method;

    static FallbackMethod withoutExceptionParameter(Method method) {
        return method == null ? null : new FallbackMethod(method, -1);
    }

    static FallbackMethod withExceptionParameter(Method method, int exceptionParameterPosition) {
        return method == null ? null : new FallbackMethod(method, exceptionParameterPosition);
    }

    private FallbackMethod(Method method, int exceptionParameterPosition) {
        this.method = method;
        this.exceptionParameterPosition = exceptionParameterPosition;
    }

    // ---

    public <T> Invoker<T> createInvoker(FailureContext ctx) throws ReflectiveOperationException {
        InvocationContext invocationContext = ctx.context.get(InvocationContext.class);
        Object[] arguments = invocationContext.getParameters();
        if (arguments == null) {
            arguments = EMPTY_ARRAY;
        }
        arguments = adjustArguments(arguments, ctx.failure);

        return method.isDefault()
                ? new SpecialMethodInvoker<>(method, invocationContext.getTarget(), arguments)
                : new NormalMethodInvoker<>(method, invocationContext.getTarget(), arguments);
    }

    private Object[] adjustArguments(Object[] arguments, Throwable exception) {
        if (method.getParameterCount() == arguments.length) {
            return arguments;
        }

        if (method.getParameterCount() == arguments.length + 1) {
            Object[] argumentsWithException = new Object[arguments.length + 1];

            int exceptionParameterPosition = this.exceptionParameterPosition;
            for (int i = 0; i < arguments.length + 1; i++) {
                if (i < exceptionParameterPosition) {
                    argumentsWithException[i] = arguments[i];
                } else if (i == exceptionParameterPosition) {
                    argumentsWithException[i] = exception;
                } else { // i > exceptionParameterPosition
                    argumentsWithException[i] = arguments[i - 1];
                }
            }

            return argumentsWithException;
        }

        throw new IllegalArgumentException("Cannot adjust arguments " + Arrays.toString(arguments)
                + " to fallback method " + method);
    }
}
