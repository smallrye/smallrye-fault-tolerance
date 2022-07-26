package io.smallrye.faulttolerance.fallbackmethod.exception.param;

public interface GenericInterface<T> {
    String fallback(T param, IllegalArgumentException e);
}
