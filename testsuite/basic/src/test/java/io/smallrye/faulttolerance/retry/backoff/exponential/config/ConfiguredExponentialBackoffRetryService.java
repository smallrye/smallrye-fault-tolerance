package io.smallrye.faulttolerance.retry.backoff.exponential.config;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.ExponentialBackoff;

@ApplicationScoped
public class ConfiguredExponentialBackoffRetryService {
    private final AtomicInteger attempts = new AtomicInteger();

    @Retry(maxRetries = -1, delay = 100, maxDuration = 800, jitter = 0)
    @ExponentialBackoff(maxDelay = 0)
    public void hello() {
        attempts.incrementAndGet();
        throw new IllegalArgumentException();
    }

    public AtomicInteger getAttempts() {
        return attempts;
    }
}
