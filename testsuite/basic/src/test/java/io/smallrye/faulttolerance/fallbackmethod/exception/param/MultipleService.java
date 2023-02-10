package io.smallrye.faulttolerance.fallbackmethod.exception.param;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;

@ApplicationScoped
public class MultipleService {
    @Fallback(fallbackMethod = "fallback")
    public String doSomething() {
        throw new IllegalArgumentException("hello");
    }

    public String fallback(IllegalArgumentException e) {
        return e.getMessage();
    }

    public String fallback(IllegalStateException e) {
        return "bad";
    }

    public String fallback(NumberFormatException e) {
        return "bad";
    }
}
