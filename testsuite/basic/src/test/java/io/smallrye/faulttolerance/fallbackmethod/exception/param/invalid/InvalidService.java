package io.smallrye.faulttolerance.fallbackmethod.exception.param.invalid;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;

@ApplicationScoped
public class InvalidService {
    @Fallback(fallbackMethod = "fallback")
    public String doSomething() {
        throw new IllegalArgumentException("hello");
    }

    public String fallback(String param, IllegalArgumentException e) {
        return e.getMessage();
    }
}
