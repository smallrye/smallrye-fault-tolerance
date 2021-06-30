package io.smallrye.faulttolerance.retry.backoff.exponential.error;

import java.time.temporal.ChronoUnit;

import javax.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.ExponentialBackoff;

@Dependent
public class MaxDelayGreaterThanMaxDurationService {
    @Retry(maxDuration = 1, durationUnit = ChronoUnit.MINUTES)
    @ExponentialBackoff(maxDelay = 2, maxDelayUnit = ChronoUnit.MINUTES)
    public void hello() {
        throw new IllegalArgumentException();
    }
}
