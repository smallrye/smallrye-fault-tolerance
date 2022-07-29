package io.smallrye.faulttolerance.fallbackmethod.exception.param;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;

@ApplicationScoped
public class SkipOnService {
    @Fallback(fallbackMethod = "fallback", skipOn = IllegalStateException.class)
    public String doSomething(boolean thrownSkipped) {
        if (thrownSkipped) {
            throw new IllegalStateException("skipped");
        } else {
            throw new IllegalArgumentException("hello");
        }
    }

    public String fallback(boolean thrownSkipped, IllegalStateException e) {
        return "bad";
    }

    public String fallback(boolean thrownSkipped, RuntimeException e) {
        return e.getMessage();
    }
}
