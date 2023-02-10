package io.smallrye.faulttolerance.retry.backoff.exponential.error;

import java.time.temporal.ChronoUnit;

import jakarta.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.ExponentialBackoff;

@Dependent
public class MaxDelayEqualToMaxDurationService {
    @Retry(maxDuration = 1, durationUnit = ChronoUnit.MINUTES)
    @ExponentialBackoff(maxDelay = 1, maxDelayUnit = ChronoUnit.MINUTES)
    public void hello() {
        throw new IllegalArgumentException();
    }
}
