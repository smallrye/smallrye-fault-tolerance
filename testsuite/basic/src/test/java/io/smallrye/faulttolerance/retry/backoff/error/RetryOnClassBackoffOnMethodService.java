package io.smallrye.faulttolerance.retry.backoff.error;

import jakarta.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.ExponentialBackoff;

@Dependent
@Retry
public class RetryOnClassBackoffOnMethodService {
    @ExponentialBackoff
    public void hello() {
        throw new IllegalArgumentException();
    }
}
