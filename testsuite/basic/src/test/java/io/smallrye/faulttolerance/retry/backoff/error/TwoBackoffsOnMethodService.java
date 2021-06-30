package io.smallrye.faulttolerance.retry.backoff.error;

import javax.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.ExponentialBackoff;
import io.smallrye.faulttolerance.api.FibonacciBackoff;

@Dependent
public class TwoBackoffsOnMethodService {
    @Retry
    @ExponentialBackoff
    @FibonacciBackoff
    public void hello() {
        throw new IllegalArgumentException();
    }
}
