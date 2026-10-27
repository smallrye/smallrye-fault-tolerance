package io.smallrye.faulttolerance.retry.metrics;

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
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.retry.metrics.RetryConfigMetricsService/overriddenMethod\".retry.max-retries", value = "21")
public class RetryConfigMetricsTest {
    @Inject
    RetryConfigMetricsService service;

    @Test
    public void plainRetry(MetricsAccess metrics) {
        service.plainMethod();

        String methodName = RetryConfigMetricsService.class.getName() + ".plainMethod";
        Attributes method = Attributes.of(stringKey("method"), methodName);

        assertThat(metrics.get(LongPointData.class, MetricsConstants.RETRY_CONFIG_MAX_RETRIES, method).getValue())
                .isEqualTo(5);
        assertThat(metrics.get(DoublePointData.class, MetricsConstants.RETRY_CONFIG_MAX_DURATION, method).getValue())
                .isEqualTo(180.0);
    }

    @Test
    public void explicitUnits(MetricsAccess metrics) {
        service.explicitUnitsMethod();

        String methodName = RetryConfigMetricsService.class.getName() + ".explicitUnitsMethod";
        Attributes method = Attributes.of(stringKey("method"), methodName);

        assertThat(metrics.get(LongPointData.class, MetricsConstants.RETRY_CONFIG_MAX_RETRIES, method).getValue())
                .isEqualTo(3);
        assertThat(metrics.get(DoublePointData.class, MetricsConstants.RETRY_CONFIG_MAX_DURATION, method).getValue())
                .isEqualTo(2.0);
    }

    @Test
    public void overriddenRetry(MetricsAccess metrics) {
        service.overriddenMethod();

        String methodName = RetryConfigMetricsService.class.getName() + ".overriddenMethod";
        Attributes method = Attributes.of(stringKey("method"), methodName);

        assertThat(metrics.get(LongPointData.class, MetricsConstants.RETRY_CONFIG_MAX_RETRIES, method).getValue())
                .isEqualTo(21);
    }
}
