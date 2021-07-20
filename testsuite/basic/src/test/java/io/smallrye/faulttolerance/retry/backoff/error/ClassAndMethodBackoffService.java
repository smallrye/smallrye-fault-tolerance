package io.smallrye.faulttolerance.retry.backoff.error;

import jakarta.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.ExponentialBackoff;
import io.smallrye.faulttolerance.api.FibonacciBackoff;

@Dependent
@Retry
@ExponentialBackoff
public class ClassAndMethodBackoffService {
    @FibonacciBackoff
    public void hello() {
        throw new IllegalArgumentException();
    }
}
