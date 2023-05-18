package io.smallrye.faulttolerance.autoconfig;

// TODO this would ideally live in the `kotlin` module
final class KotlinSupport {
    private static final String KOTLIN_CONTINUATION = "kotlin.coroutines.Continuation";

    static boolean isLegitimate(MethodDescriptor method) {
        if (method.parameterTypes.length > 0
                && method.parameterTypes[method.parameterTypes.length - 1].getName().equals(KOTLIN_CONTINUATION)
                && method.name.endsWith("$suspendImpl")) {
            return false;
        }
        return true;
    }
}
