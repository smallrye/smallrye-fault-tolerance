package io.smallrye.faulttolerance.timeout.metrics;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.smallrye.faulttolerance.core.metrics.MetricsConstants;
import io.smallrye.faulttolerance.minimptel.MetricsAccess;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.timeout.metrics.TimeoutConfigMetricsService/overriddenMethod\".timeout.value", value = "750")
public class TimeoutConfigMetricsTest {
    @Inject
    TimeoutConfigMetricsService service;

    @Test
    public void plainTimeout(MetricsAccess metrics) {
        service.plainMethod();

        String methodName = TimeoutConfigMetricsService.class.getName() + ".plainMethod";
        Attributes method = Attributes.of(stringKey("method"), methodName);

        assertThat(metrics.get(DoublePointData.class, MetricsConstants.TIMEOUT_CONFIG_DURATION, method).getValue())
                .isEqualTo(0.25);
    }

    @Test
    public void explicitUnit(MetricsAccess metrics) {
        service.explicitUnitMethod();

        String methodName = TimeoutConfigMetricsService.class.getName() + ".explicitUnitMethod";
        Attributes method = Attributes.of(stringKey("method"), methodName);

        assertThat(metrics.get(DoublePointData.class, MetricsConstants.TIMEOUT_CONFIG_DURATION, method).getValue())
                .isEqualTo(2.0);
    }

    @Test
    public void overriddenTimeout(MetricsAccess metrics) {
        service.overriddenMethod();

        String methodName = TimeoutConfigMetricsService.class.getName() + ".overriddenMethod";
        Attributes method = Attributes.of(stringKey("method"), methodName);

        assertThat(metrics.get(DoublePointData.class, MetricsConstants.TIMEOUT_CONFIG_DURATION, method).getValue())
                .isEqualTo(0.75);
    }
}
