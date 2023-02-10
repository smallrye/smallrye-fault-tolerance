package io.smallrye.faulttolerance.retry.backoff.fibonacci;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.FibonacciBackoff;

@ApplicationScoped
public class FibonacciBackoffRetryService {
    private final AtomicInteger attempts = new AtomicInteger();

    @Retry(maxRetries = -1, delay = 100, maxDuration = 800, jitter = 0)
    @FibonacciBackoff(maxDelay = 0)
    public void hello() {
        attempts.incrementAndGet();
        throw new IllegalArgumentException();
    }

    public AtomicInteger getAttempts() {
        return attempts;
    }
}
