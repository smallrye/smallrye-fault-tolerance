package io.smallrye.faulttolerance.stereotype;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;

@CompositeStereotype
@ApplicationScoped
public class NestedStereotypeService {

    private final AtomicInteger callCount = new AtomicInteger(0);

    AtomicInteger getCallCount() {
        return callCount;
    }

    void resetCallCount() {
        callCount.set(0);
    }

    @Fallback(fallbackMethod = "fallback")
    public String work() {
        callCount.incrementAndGet();
        throw new RuntimeException("transient failure");
    }

    String fallback() {
        return "fallback";
    }
}
