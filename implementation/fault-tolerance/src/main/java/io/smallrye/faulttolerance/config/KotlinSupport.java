package io.smallrye.faulttolerance.config;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

// TODO this would ideally live in the `kotlin` module
final class KotlinSupport {
    static boolean isSuspendingFunction(Method method) {
        int params = method.getParameterCount();
        return params > 0 && method.getParameterTypes()[params - 1].getName().equals("kotlin.coroutines.Continuation");
    }

    static Type getSuspendingFunctionResultType(Method method) {
        if (!isSuspendingFunction(method)) {
            throw new IllegalArgumentException("Not a suspend function: " + method);
        }

        Type lastParameter = method.getGenericParameterTypes()[method.getParameterCount() - 1];
        if (!(lastParameter instanceof ParameterizedType)) {
            throw new IllegalArgumentException("Continuation parameter type not parameterized: " + lastParameter);
        }
        Type resultType = ((ParameterizedType) lastParameter).getActualTypeArguments()[0];
        if (!(resultType instanceof WildcardType)) {
            throw new IllegalArgumentException("Continuation parameter type argument not wildcard: " + resultType);
        }
        Type[] lowerBounds = ((WildcardType) resultType).getLowerBounds();
        if (lowerBounds.length == 0) {
            throw new IllegalArgumentException("Continuation parameter type argument without lower bound: " + resultType);
        }
        return lowerBounds[0];
    }
}
