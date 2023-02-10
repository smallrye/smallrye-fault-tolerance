package io.smallrye.faulttolerance.retry.backoff.fibonacci.error;

import java.time.temporal.ChronoUnit;

import jakarta.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.FibonacciBackoff;

@Dependent
public class MaxDelayEqualToMaxDurationService {
    @Retry(maxDuration = 1, durationUnit = ChronoUnit.MINUTES)
    @FibonacciBackoff(maxDelay = 1, maxDelayUnit = ChronoUnit.MINUTES)
    public void hello() {
        throw new IllegalArgumentException();
    }
}
