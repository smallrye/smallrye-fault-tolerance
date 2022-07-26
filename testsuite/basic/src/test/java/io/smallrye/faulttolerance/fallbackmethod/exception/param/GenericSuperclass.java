package io.smallrye.faulttolerance.fallbackmethod.exception.param;

import org.eclipse.microprofile.faulttolerance.Fallback;

public abstract class GenericSuperclass<T> implements GenericInterface<T> {
    @Fallback(fallbackMethod = "fallback")
    public String doSomething(T param) {
        throw new IllegalArgumentException("hello");
    }
}
