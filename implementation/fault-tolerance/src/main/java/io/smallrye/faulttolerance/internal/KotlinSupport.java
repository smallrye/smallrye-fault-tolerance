package io.smallrye.faulttolerance.internal;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

// TODO this would ideally live in the `kotlin` module
final class KotlinSupport {
    private static final String KOTLIN_CONTINUATION = "kotlin.coroutines.Continuation";

    static boolean isSuspendingFunction(Method method) {
        int params = method.getParameterCount();
        return params > 0 && method.getParameterTypes()[params - 1].getName().equals(KOTLIN_CONTINUATION);
    }

    static boolean isSuspendingFunction(Type[] parameterTypes) {
        int params = parameterTypes.length;
        if (params > 0) {
            Type last = parameterTypes[params - 1];
            return last instanceof Class && last.getTypeName().equals(KOTLIN_CONTINUATION)
                    || last instanceof ParameterizedType && last.getTypeName().startsWith(KOTLIN_CONTINUATION);
        }
        return false;
    }
}
