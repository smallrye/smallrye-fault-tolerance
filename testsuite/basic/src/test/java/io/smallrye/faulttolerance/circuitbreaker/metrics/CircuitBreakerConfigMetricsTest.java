package io.smallrye.faulttolerance.circuitbreaker.metrics;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.smallrye.faulttolerance.core.metrics.MetricsConstants;
import io.smallrye.faulttolerance.minimptel.MetricsAccess;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.circuitbreaker.metrics.CircuitBreakerConfigMetricsService/overriddenMethod\".circuit-breaker.delay", value = "750")
public class CircuitBreakerConfigMetricsTest {
    @Inject
    CircuitBreakerConfigMetricsService service;

    @Test
    public void plainCircuitBreaker(MetricsAccess metrics) {
        service.plainMethod();

        String methodName = CircuitBreakerConfigMetricsService.class.getName() + ".plainMethod";
        Attributes method = Attributes.of(stringKey("method"), methodName);

        assertThat(metrics.get(LongPointData.class, MetricsConstants.CIRCUIT_BREAKER_CONFIG_DELAY, method).getValue())
                .isEqualTo(5_000_000_000L);
        assertThat(metrics.get(LongPointData.class, MetricsConstants.CIRCUIT_BREAKER_CONFIG_REQUEST_VOLUME_THRESHOLD, method)
                .getValue()).isEqualTo(20);
        assertThat(metrics.get(DoublePointData.class, MetricsConstants.CIRCUIT_BREAKER_CONFIG_FAILURE_RATIO, method).getValue())
                .isEqualTo(0.5);
        assertThat(metrics.get(LongPointData.class, MetricsConstants.CIRCUIT_BREAKER_CONFIG_SUCCESS_THRESHOLD, method)
                .getValue()).isEqualTo(1);
    }

    @Test
    public void explicitUnits(MetricsAccess metrics) {
        service.explicitUnitsMethod();

        String methodName = CircuitBreakerConfigMetricsService.class.getName() + ".explicitUnitsMethod";
        Attributes method = Attributes.of(stringKey("method"), methodName);

        assertThat(metrics.get(LongPointData.class, MetricsConstants.CIRCUIT_BREAKER_CONFIG_DELAY, method).getValue())
                .isEqualTo(2_000_000_000L);
        assertThat(metrics.get(LongPointData.class, MetricsConstants.CIRCUIT_BREAKER_CONFIG_REQUEST_VOLUME_THRESHOLD, method)
                .getValue()).isEqualTo(10);
        assertThat(metrics.get(DoublePointData.class, MetricsConstants.CIRCUIT_BREAKER_CONFIG_FAILURE_RATIO, method).getValue())
                .isEqualTo(0.25);
        assertThat(metrics.get(LongPointData.class, MetricsConstants.CIRCUIT_BREAKER_CONFIG_SUCCESS_THRESHOLD, method)
                .getValue()).isEqualTo(3);
    }

    @Test
    public void overriddenCircuitBreaker(MetricsAccess metrics) {
        service.overriddenMethod();

        String methodName = CircuitBreakerConfigMetricsService.class.getName() + ".overriddenMethod";
        Attributes method = Attributes.of(stringKey("method"), methodName);

        assertThat(metrics.get(LongPointData.class, MetricsConstants.CIRCUIT_BREAKER_CONFIG_DELAY, method).getValue())
                .isEqualTo(750_000_000L);
    }
}
