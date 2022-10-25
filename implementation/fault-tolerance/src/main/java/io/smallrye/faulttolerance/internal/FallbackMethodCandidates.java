package io.smallrye.faulttolerance.internal;

import java.lang.reflect.Method;
import java.security.PrivilegedActionException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

public final class FallbackMethodCandidates {
    private final Method withoutExceptionParam;
    private final Map<Class<?>, Method> withExceptionParam;

    private FallbackMethodCandidates(Method withoutExceptionParam, Set<Method> withExceptionParam) {
        this.withoutExceptionParam = withoutExceptionParam;

        Map<Class<?>, Method> map = new HashMap<>();
        for (Method method : withExceptionParam) {
            map.put(method.getParameterTypes()[method.getParameterCount() - 1], method);
        }
        this.withExceptionParam = map;
    }

    public boolean isEmpty() {
        return withoutExceptionParam == null && withExceptionParam.isEmpty();
    }

    public Method select(Class<? extends Throwable> exceptionType) {
        if (!withExceptionParam.isEmpty()) {
            Class<?> type = exceptionType;
            while (type != null) {
                Method candidate = withExceptionParam.get(type);
                if (candidate != null) {
                    return candidate;
                }
                type = type.getSuperclass();
            }
        }

        // may be null
        return withoutExceptionParam;
    }

    public static FallbackMethodCandidates create(InterceptionPoint point, String fallbackMethodName,
            boolean allowExceptionParam) {
        try {
            Method guardedMethod = point.method();

            Method withoutExceptionParam = SecurityActions.findFallbackMethod(point.beanClass(),
                    guardedMethod.getDeclaringClass(), fallbackMethodName,
                    guardedMethod.getGenericParameterTypes(), guardedMethod.getGenericReturnType());
            if (withoutExceptionParam != null) {
                SecurityActions.setAccessible(withoutExceptionParam);
            }

            Set<Method> withExceptionParam = Collections.emptySet();
            if (allowExceptionParam) {
                withExceptionParam = SecurityActions.findFallbackMethodsWithExceptionParammeter(point.beanClass(),
                        guardedMethod.getDeclaringClass(), fallbackMethodName,
                        guardedMethod.getGenericParameterTypes(), guardedMethod.getGenericReturnType());
                for (Method method : withExceptionParam) {
                    SecurityActions.setAccessible(method);
                }
            }

            return new FallbackMethodCandidates(withoutExceptionParam, withExceptionParam);
        } catch (PrivilegedActionException e) {
            throw new FaultToleranceException("Could not obtain fallback method " + fallbackMethodName, e);
        }
    }
}
