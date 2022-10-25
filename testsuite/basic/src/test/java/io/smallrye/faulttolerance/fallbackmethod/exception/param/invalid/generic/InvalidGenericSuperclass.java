package io.smallrye.faulttolerance.fallbackmethod.exception.param.invalid.generic;

import org.eclipse.microprofile.faulttolerance.Fallback;

public abstract class InvalidGenericSuperclass<T> {
    @Fallback(fallbackMethod = "fallback")
    public String doSomething(T param) {
        throw new IllegalArgumentException("hello");
    }
}
