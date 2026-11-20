package io.smallrye.faulttolerance.retry.backoff.custom.compat;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.CustomBackoff;
import io.smallrye.faulttolerance.api.RetryWhen;
import io.smallrye.faulttolerance.retry.backoff.custom.IsNotString;

@ApplicationScoped
public class CustomBackoffRetryService {
    static final AtomicInteger attempts = new AtomicInteger();

    @Retry(maxRetries = 2, delay = 100_000, delayUnit = ChronoUnit.MICROS, jitter = 0)
    @CustomBackoff(TestBackoffStrategy.class)
    public void helloWithFailures() {
        int num = attempts.incrementAndGet();
        if (num == 1) {
            throw new IllegalArgumentException();
        } else if (num == 2) {
            throw new IllegalStateException();
        } else {
            throw new NullPointerException();
        }
    }

    @Retry(maxRetries = 2, delay = 10_000, delayUnit = ChronoUnit.MICROS, jitter = 0)
    @RetryWhen(result = IsNotString.class)
    @CustomBackoff(TestBackoffStrategy.class)
    public Object helloWithResults() {
        int num = attempts.incrementAndGet();
        if (num == 1) {
            return 0;
        } else if (num == 2) {
            return 0.0;
        } else {
            return "hello";
        }
    }
}
