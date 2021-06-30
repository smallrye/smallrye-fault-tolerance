package io.smallrye.faulttolerance.retry.backoff.exponential.error;

import javax.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.ExponentialBackoff;

@Dependent
public class NegativeFactorService {
    @Retry
    @ExponentialBackoff(factor = -1)
    public void hello() {
        throw new IllegalArgumentException();
    }
}
