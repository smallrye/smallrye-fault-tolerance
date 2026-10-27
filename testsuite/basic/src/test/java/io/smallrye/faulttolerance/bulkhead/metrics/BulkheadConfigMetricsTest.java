package io.smallrye.faulttolerance.bulkhead.metrics;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.smallrye.faulttolerance.core.metrics.MetricsConstants;
import io.smallrye.faulttolerance.minimptel.MetricsAccess;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@WithSystemProperty(key = "smallrye.faulttolerance.\"io.smallrye.faulttolerance.bulkhead.metrics.BulkheadConfigMetricsService/overriddenMethod\".bulkhead.value", value = "13")
public class BulkheadConfigMetricsTest {
    @Inject
    BulkheadConfigMetricsService service;

    @Test
    public void syncBulkhead(MetricsAccess metrics) {
        service.syncMethod();

        String methodName = BulkheadConfigMetricsService.class.getName() + ".syncMethod";
        Attributes method = Attributes.of(stringKey("method"), methodName);

        assertThat(metrics.get(LongPointData.class, MetricsConstants.BULKHEAD_CONFIG_LIMIT, method).getValue())
                .isEqualTo(3);
        // the waiting queue is only meaningful for asynchronous methods
        assertThat(metrics.get(LongPointData.class, MetricsConstants.BULKHEAD_CONFIG_QUEUE_SIZE, method))
                .isNull();
    }

    @Test
    public void asyncBulkhead(MetricsAccess metrics) throws Exception {
        service.asyncMethod().toCompletableFuture().get();

        String methodName = BulkheadConfigMetricsService.class.getName() + ".asyncMethod";
        Attributes method = Attributes.of(stringKey("method"), methodName);

        assertThat(metrics.get(LongPointData.class, MetricsConstants.BULKHEAD_CONFIG_LIMIT, method).getValue())
                .isEqualTo(5);
        assertThat(metrics.get(LongPointData.class, MetricsConstants.BULKHEAD_CONFIG_QUEUE_SIZE, method).getValue())
                .isEqualTo(6);
    }

    @Test
    public void overriddenBulkhead(MetricsAccess metrics) {
        service.overriddenMethod();

        String methodName = BulkheadConfigMetricsService.class.getName() + ".overriddenMethod";
        Attributes method = Attributes.of(stringKey("method"), methodName);

        assertThat(metrics.get(LongPointData.class, MetricsConstants.BULKHEAD_CONFIG_LIMIT, method).getValue())
                .isEqualTo(13);
    }
}
