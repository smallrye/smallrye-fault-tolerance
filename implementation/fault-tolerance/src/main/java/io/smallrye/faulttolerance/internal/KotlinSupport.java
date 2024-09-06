package io.smallrye.faulttolerance.internal;

import java.lang.reflect.Method;

// TODO this would ideally live in the `kotlin` module
final class KotlinSupport {
    private static final String KOTLIN_CONTINUATION = "kotlin.coroutines.Continuation";

    static boolean isSuspendingFunction(Method method) {
        int params = method.getParameterCount();
        return params > 0 && method.getParameterTypes()[params - 1].getName().equals(KOTLIN_CONTINUATION);
    }
}
