package io.smallrye.faulttolerance.stereotype;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.faulttolerance.Fallback;

public abstract class ServiceBase {
    private final AtomicInteger invocations = new AtomicInteger();

    void reset() {
        invocations.set(0);
    }

    int getInvocations() {
        return invocations.get();
    }

    @Fallback(fallbackMethod = "fallback")
    public String hello() {
        invocations.incrementAndGet();
        throw new RuntimeException("simulated failure");
    }

    public String fallback() {
        return "fallback";
    }
}
