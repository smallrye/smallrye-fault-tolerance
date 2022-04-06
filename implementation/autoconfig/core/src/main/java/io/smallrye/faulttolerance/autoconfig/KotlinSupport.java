package io.smallrye.faulttolerance.autoconfig;

// TODO this would ideally live in the `kotlin` module
final class KotlinSupport {
    static boolean isLegitimate(MethodDescriptor method) {
        if (method.parameterTypes.length > 0
                && method.parameterTypes[method.parameterTypes.length - 1].getName().equals("kotlin.coroutines.Continuation")
                && method.name.endsWith("$suspendImpl")) {
            return false;
        }
        return true;
    }
}
