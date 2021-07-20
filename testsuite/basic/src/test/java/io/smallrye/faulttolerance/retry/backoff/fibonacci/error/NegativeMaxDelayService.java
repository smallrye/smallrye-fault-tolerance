package io.smallrye.faulttolerance.retry.backoff.fibonacci.error;

import jakarta.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.FibonacciBackoff;

@Dependent
public class NegativeMaxDelayService {
    @Retry
    @FibonacciBackoff(maxDelay = -1)
    public void hello() {
        throw new IllegalArgumentException();
    }
}
