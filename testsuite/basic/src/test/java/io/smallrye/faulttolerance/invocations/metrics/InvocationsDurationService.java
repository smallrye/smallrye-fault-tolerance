package io.smallrye.faulttolerance.invocations.metrics;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

@ApplicationScoped
public class InvocationsDurationService {
    @Retry(maxRetries = 3)
    public void plainMethod() {
    }

    private final AtomicInteger counter = new AtomicInteger();

    @Retry(maxRetries = 3, delay = 300, jitter = 0)
    public void retriedMethod() {
        if (counter.incrementAndGet() < 3) {
            throw new RuntimeException();
        }
    }
}
