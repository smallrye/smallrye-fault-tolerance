package io.smallrye.faulttolerance.ratelimit.metrics;

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
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.ratelimit.metrics.RateLimitConfigMetricsService/overriddenMethod\".rate-limit.value", value = "21")
public class RateLimitConfigMetricsTest {
    @Inject
    RateLimitConfigMetricsService service;

    @Test
    public void plainRateLimit(MetricsAccess metrics) {
        service.plainMethod();

        String methodName = RateLimitConfigMetricsService.class.getName() + ".plainMethod";
        Attributes method = Attributes.of(stringKey("method"), methodName);

        assertThat(metrics.get(LongPointData.class, MetricsConstants.RATE_LIMIT_CONFIG_LIMIT, method).getValue())
                .isEqualTo(5);
        assertThat(metrics.get(DoublePointData.class, MetricsConstants.RATE_LIMIT_CONFIG_WINDOW, method).getValue())
                .isEqualTo(1.0);
        assertThat(metrics.get(DoublePointData.class, MetricsConstants.RATE_LIMIT_CONFIG_MIN_SPACING, method).getValue())
                .isEqualTo(0.0);
    }

    @Test
    public void explicitUnits(MetricsAccess metrics) {
        service.explicitUnitsMethod();

        String methodName = RateLimitConfigMetricsService.class.getName() + ".explicitUnitsMethod";
        Attributes method = Attributes.of(stringKey("method"), methodName);

        assertThat(metrics.get(LongPointData.class, MetricsConstants.RATE_LIMIT_CONFIG_LIMIT, method).getValue())
                .isEqualTo(10);
        assertThat(metrics.get(DoublePointData.class, MetricsConstants.RATE_LIMIT_CONFIG_WINDOW, method).getValue())
                .isEqualTo(2.0);
        assertThat(metrics.get(DoublePointData.class, MetricsConstants.RATE_LIMIT_CONFIG_MIN_SPACING, method).getValue())
                .isEqualTo(0.5);
    }

    @Test
    public void overriddenRateLimit(MetricsAccess metrics) {
        service.overriddenMethod();

        String methodName = RateLimitConfigMetricsService.class.getName() + ".overriddenMethod";
        Attributes method = Attributes.of(stringKey("method"), methodName);

        assertThat(metrics.get(LongPointData.class, MetricsConstants.RATE_LIMIT_CONFIG_LIMIT, method).getValue())
                .isEqualTo(21);
    }
}
