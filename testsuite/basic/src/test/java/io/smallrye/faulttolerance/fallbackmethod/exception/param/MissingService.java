package io.smallrye.faulttolerance.fallbackmethod.exception.param;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;

@ApplicationScoped
public class MissingService {
    @Fallback(fallbackMethod = "fallback")
    public String doSomething() {
        throw new RuntimeException("hello");
    }

    public String fallback(IllegalArgumentException e) {
        return "bad";
    }

    public String fallback(IllegalStateException e) {
        return "bad";
    }

    public String fallback(NumberFormatException e) {
        return "bad";
    }
}
