package io.smallrye.faulttolerance.fallbackmethod.exception.param;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;

@ApplicationScoped
public class MixedService {
    @Fallback(fallbackMethod = "fallback")
    public String doSomething(boolean throwKnown) {
        if (throwKnown) {
            throw new IllegalArgumentException("hello");
        } else {
            throw new RuntimeException();
        }
    }

    public String fallback(boolean throwKnown, IllegalArgumentException e) {
        return e.getMessage();
    }

    public String fallback(boolean throwKnown, IllegalStateException e) {
        return "bad";
    }

    public String fallback(boolean throwKnown, NumberFormatException e) {
        return "bad";
    }

    public String fallback(boolean throwKnown) {
        return "fallback";
    }
}
