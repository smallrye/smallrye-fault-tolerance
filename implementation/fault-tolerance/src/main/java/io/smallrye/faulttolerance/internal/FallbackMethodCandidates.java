package io.smallrye.faulttolerance.internal;

import java.lang.reflect.Method;
import java.security.PrivilegedActionException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

public final class FallbackMethodCandidates {
    private final FallbackMethod withoutExceptionParam;
    private final Map<Class<?>, FallbackMethod> withExceptionParam;

    private FallbackMethodCandidates(Method withoutExceptionParam, Set<Method> withExceptionParam) {
        this.withoutExceptionParam = FallbackMethod.withoutExceptionParameter(withoutExceptionParam);

        Map<Class<?>, FallbackMethod> map = new HashMap<>();
        for (Method method : withExceptionParam) {
            int exceptionParameterPosition = method.getParameterCount() - 1;
            if (KotlinSupport.isSuspendingFunction(method)) {
                exceptionParameterPosition--;
            }

            map.put(method.getParameterTypes()[exceptionParameterPosition],
                    FallbackMethod.withExceptionParameter(method, exceptionParameterPosition));
        }
        this.withExceptionParam = map;
    }

    public boolean isEmpty() {
        return withoutExceptionParam == null && withExceptionParam.isEmpty();
    }

    public FallbackMethod select(Class<? extends Throwable> exceptionType) {
        if (!withExceptionParam.isEmpty()) {
            Class<?> type = exceptionType;
            while (type != null) {
                FallbackMethod candidate = withExceptionParam.get(type);
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
                withExceptionParam = SecurityActions.findFallbackMethodsWithExceptionParameter(point.beanClass(),
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
