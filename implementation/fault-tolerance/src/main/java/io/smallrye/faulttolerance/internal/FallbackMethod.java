package io.smallrye.faulttolerance.internal;

import java.lang.reflect.Method;
import java.util.Arrays;

import jakarta.interceptor.InvocationContext;

import io.smallrye.faulttolerance.core.fallback.FallbackContext;
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

    public Invoker<?> createInvoker(FallbackContext<?> ctx) throws ReflectiveOperationException {
        InvocationContext interceptionContext = ctx.invocationContext.get(InvocationContext.class);
        Object[] arguments = interceptionContext.getParameters();
        if (arguments == null) {
            arguments = EMPTY_ARRAY;
        }
        arguments = adjustArguments(arguments, ctx.failure);

        return method.isDefault()
                ? new SpecialMethodInvoker<>(method, interceptionContext.getTarget(), arguments)
                : new NormalMethodInvoker<>(method, interceptionContext.getTarget(), arguments);
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
