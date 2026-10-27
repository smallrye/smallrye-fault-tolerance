package io.smallrye.faulttolerance.retry.metrics;

import java.time.temporal.ChronoUnit;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

@ApplicationScoped
public class RetryConfigMetricsService {
    @Retry(maxRetries = 5)
    public void plainMethod() {
    }

    @Retry(maxRetries = 3, maxDuration = 2, durationUnit = ChronoUnit.SECONDS)
    public void explicitUnitsMethod() {
    }

    @Retry(maxRetries = 7)
    public void overriddenMethod() {
    }
}
