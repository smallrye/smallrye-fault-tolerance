package io.smallrye.faulttolerance.retry.backoff.custom;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.CustomBackoff;

@ApplicationScoped
public class CustomBackoffRetryService {
    private final AtomicInteger attempts = new AtomicInteger();

    @Retry(maxRetries = -1, delay = 100_000, delayUnit = ChronoUnit.MICROS, maxDuration = 600)
    @CustomBackoff(TestBackoffStrategy.class)
    public void hello() {
        int num = attempts.incrementAndGet();
        if (num == 1) {
            throw new IllegalArgumentException();
        } else if (num == 2) {
            throw new IllegalStateException();
        } else if (num == 3) {
            throw new NullPointerException();
        } else if (num == 4) {
            throw new UnsupportedOperationException();
        } else {
            throw new ArithmeticException();
        }
    }

    public AtomicInteger getAttempts() {
        return attempts;
    }
}
