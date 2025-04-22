package io.smallrye.faulttolerance.retry.stackoverflow;

import jakarta.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

@Dependent
public class MyService {
    @Retry(maxRetries = 10_000, jitter = 0)
    @Fallback(fallbackMethod = "fallback")
    public String hello() {
        throw new RuntimeException("force retry");
    }

    public String fallback() {
        return "fallback";
    }
}
