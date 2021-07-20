package io.smallrye.faulttolerance.retry.backoff.exponential.error;

import jakarta.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.ExponentialBackoff;

@Dependent
public class ZeroFactorService {
    @Retry
    @ExponentialBackoff(factor = 0)
    public void hello() {
        throw new IllegalArgumentException();
    }
}
