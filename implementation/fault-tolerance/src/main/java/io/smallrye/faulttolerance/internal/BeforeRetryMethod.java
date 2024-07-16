package io.smallrye.faulttolerance.internal;

import java.lang.reflect.Method;
import java.security.PrivilegedActionException;

import jakarta.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

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

    public static BeforeRetryMethod find(InterceptionPoint point, String beforeRetryMethodName) {
        try {
            Method beforeRetryMethod = SecurityActions.findBeforeRetryMethod(point.beanClass(),
                    point.method().getDeclaringClass(), beforeRetryMethodName);
            if (beforeRetryMethod != null) {
                SecurityActions.setAccessible(beforeRetryMethod);
                return new BeforeRetryMethod(beforeRetryMethod);
            }
            return null;
        } catch (PrivilegedActionException e) {
            throw new FaultToleranceException("Could not obtain before retry method " + beforeRetryMethodName, e);
        }
    }
}
