package io.smallrye.faulttolerance.internal;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.smallrye.faulttolerance.config.FaultToleranceOperation;

public final class FallbackMethodCandidates {
    private final FallbackMethod withoutExceptionParam;
    private final Map<Class<?>, FallbackMethod> withExceptionParam;

    private FallbackMethodCandidates(Method withoutExceptionParam, List<Method> withExceptionParam) {
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

    public static FallbackMethodCandidates create(FaultToleranceOperation operation, boolean allowExceptionParam) {
        Method withoutExceptionParam = operation.getFallbackMethod();
        List<Method> withExceptionParam = Collections.emptyList();
        if (allowExceptionParam && operation.getFallbackMethodsWithExceptionParameter() != null) {
            withExceptionParam = operation.getFallbackMethodsWithExceptionParameter();
        }

        return new FallbackMethodCandidates(withoutExceptionParam, withExceptionParam);
    }
}
