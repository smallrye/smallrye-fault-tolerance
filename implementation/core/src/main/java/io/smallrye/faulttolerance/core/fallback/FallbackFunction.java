package io.smallrye.faulttolerance.core.fallback;

@FunctionalInterface
public interface FallbackFunction<T> {
    T call(Throwable failure) throws Exception;
}
