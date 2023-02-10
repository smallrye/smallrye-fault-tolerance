package io.smallrye.faulttolerance.retry.backoff.error;

import jakarta.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.FibonacciBackoff;

@Dependent
@FibonacciBackoff
public class RetryOnMethodBackoffOnClassService {
    @Retry
    public void hello() {
        throw new IllegalArgumentException();
    }
}
