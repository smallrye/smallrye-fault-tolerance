package io.smallrye.faulttolerance.invocations.metrics;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.smallrye.faulttolerance.core.metrics.MetricsConstants;
import io.smallrye.faulttolerance.minimptel.MetricsAccess;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class InvocationsDurationTest {
    @Inject
    InvocationsDurationService service;

    @Test
    public void plainInvocation(MetricsAccess metrics) {
        service.plainMethod();

        String methodName = InvocationsDurationService.class.getName() + ".plainMethod";
        Attributes method = Attributes.of(stringKey("method"), methodName);

        assertThat(metrics.get(HistogramPointData.class, MetricsConstants.INVOCATIONS_DURATION, method).getCount())
                .isEqualTo(1);
    }

    @Test
    public void retriedInvocation(MetricsAccess metrics) {
        // the method fails twice and then succeeds, so the guarded execution
        // spends at least 600 ms in retry delays (2 x 300 ms)
        service.retriedMethod();

        String methodName = InvocationsDurationService.class.getName() + ".retriedMethod";
        Attributes method = Attributes.of(stringKey("method"), methodName);

        HistogramPointData point = metrics.get(HistogramPointData.class,
                MetricsConstants.INVOCATIONS_DURATION, method);

        assertThat(point.getCount()).isEqualTo(1);
        assertThat(point.getSum()).isGreaterThanOrEqualTo(0.6);
    }
}
