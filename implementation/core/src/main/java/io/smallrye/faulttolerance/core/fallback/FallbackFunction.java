package io.smallrye.faulttolerance.core.fallback;

@FunctionalInterface
public interface FallbackFunction<T> {
    T call(FallbackContext<T> ctx) throws Exception;
}
