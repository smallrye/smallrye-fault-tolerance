package io.smallrye.faulttolerance.internal;

import java.lang.reflect.Method;

import jakarta.interceptor.InvocationContext;

import io.smallrye.faulttolerance.config.FaultToleranceOperation;
import io.smallrye.faulttolerance.core.FailureContext;
import io.smallrye.faulttolerance.core.invocation.Invoker;
import io.smallrye.faulttolerance.core.invocation.NormalMethodInvoker;
import io.smallrye.faulttolerance.core.invocation.SpecialMethodInvoker;

public final class BeforeRetryMethod {
    private static final Object[] EMPTY_ARRAY = {};

    private final Method method;

    BeforeRetryMethod(Method method) {
        this.method = method;
    }

    public Invoker<?> createInvoker(FailureContext ctx) throws ReflectiveOperationException {
        InvocationContext interceptionContext = ctx.invocationContext.get(InvocationContext.class);
        Object[] arguments = interceptionContext.getParameters();
        if (arguments == null) {
            arguments = EMPTY_ARRAY;
        }

        return method.isDefault()
                ? new SpecialMethodInvoker<>(method, interceptionContext.getTarget(), arguments)
                : new NormalMethodInvoker<>(method, interceptionContext.getTarget(), arguments);
    }

    // ---

    public static BeforeRetryMethod create(FaultToleranceOperation operation) {
        Method beforeRetryMethod = operation.getBeforeRetryMethod();
        return beforeRetryMethod != null ? new BeforeRetryMethod(beforeRetryMethod) : null;
    }
}
