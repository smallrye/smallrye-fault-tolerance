package io.smallrye.faulttolerance.timeout.metrics;

import java.time.temporal.ChronoUnit;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Timeout;

@ApplicationScoped
public class TimeoutConfigMetricsService {
    @Timeout(250)
    public void plainMethod() {
    }

    @Timeout(value = 2, unit = ChronoUnit.SECONDS)
    public void explicitUnitMethod() {
    }

    @Timeout(500)
    public void overriddenMethod() {
    }
}
