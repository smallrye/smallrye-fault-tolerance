package io.smallrye.faulttolerance.fallbackmethod.exception.param;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;

@ApplicationScoped
public class BasicService {
    @Fallback(fallbackMethod = "fallback")
    public String doSomething() {
        throw new IllegalArgumentException("hello");
    }

    public String fallback(Exception e) {
        return e.getMessage();
    }
}
