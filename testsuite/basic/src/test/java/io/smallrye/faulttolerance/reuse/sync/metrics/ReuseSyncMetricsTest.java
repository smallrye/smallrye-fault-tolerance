package io.smallrye.faulttolerance.reuse.sync.metrics;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.smallrye.faulttolerance.minimptel.MetricsAccess;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
public class ReuseSyncMetricsTest {
    @Test
    public void test(MyService service, MetricsAccess metrics) {
        assertThat(service.first()).isEqualTo("fallback");
        assertThat(service.second()).isEqualTo("fallback");
        assertThat(service.second()).isEqualTo("fallback");
        assertThat(service.second()).isEqualTo("fallback");

        // first

        assertThat(metrics.get(LongPointData.class, "ft.invocations.total", Attributes.of(
                stringKey("method"), "io.smallrye.faulttolerance.reuse.sync.metrics.MyService.first",
                stringKey("result"), "valueReturned",
                stringKey("fallback"), "applied"))
                .getValue()).isEqualTo(1);

        assertThat(metrics.get(LongPointData.class, "ft.retry.retries.total", Attributes.of(
                stringKey("method"), "io.smallrye.faulttolerance.reuse.sync.metrics.MyService.first"))
                .getValue()).isEqualTo(2);
        assertThat(metrics.get(LongPointData.class, "ft.retry.calls.total", Attributes.of(
                stringKey("method"), "io.smallrye.faulttolerance.reuse.sync.metrics.MyService.first",
                stringKey("retried"), "true",
                stringKey("retryResult"), "maxRetriesReached"))
                .getValue()).isEqualTo(1);

        // second

        assertThat(metrics.get(LongPointData.class, "ft.invocations.total", Attributes.of(
                stringKey("method"), "io.smallrye.faulttolerance.reuse.sync.metrics.MyService.second",
                stringKey("result"), "valueReturned",
                stringKey("fallback"), "applied"))
                .getValue()).isEqualTo(3);

        assertThat(metrics.get(LongPointData.class, "ft.retry.retries.total", Attributes.of(
                stringKey("method"), "io.smallrye.faulttolerance.reuse.sync.metrics.MyService.second"))
                .getValue()).isEqualTo(6);
        assertThat(metrics.get(LongPointData.class, "ft.retry.calls.total", Attributes.of(
                stringKey("method"), "io.smallrye.faulttolerance.reuse.sync.metrics.MyService.second",
                stringKey("retried"), "true",
                stringKey("retryResult"), "maxRetriesReached"))
                .getValue()).isEqualTo(3);
    }
}
