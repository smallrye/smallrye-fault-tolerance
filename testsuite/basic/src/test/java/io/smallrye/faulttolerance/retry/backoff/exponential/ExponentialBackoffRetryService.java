package io.smallrye.faulttolerance.retry.backoff.exponential;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.ExponentialBackoff;

@ApplicationScoped
public class ExponentialBackoffRetryService {
    private final AtomicInteger attempts = new AtomicInteger();

    @Retry(maxRetries = -1, delay = 100, maxDuration = 1000, jitter = 0)
    @ExponentialBackoff(maxDelay = 0)
    public void hello() {
        attempts.incrementAndGet();
        throw new IllegalArgumentException();
    }

    public AtomicInteger getAttempts() {
        return attempts;
    }
}
