package io.smallrye.faulttolerance.reuse.sync.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
public class ReuseSyncMetricsTest {
    @Test
    public void test(MyService service, @RegistryType(type = MetricRegistry.Type.BASE) MetricRegistry metrics) {
        assertThat(service.first()).isEqualTo("fallback");
        assertThat(service.second()).isEqualTo("fallback");
        assertThat(service.second()).isEqualTo("fallback");
        assertThat(service.second()).isEqualTo("fallback");

        // first

        assertThat(metrics.counter("ft.invocations.total",
                new Tag("method", "io.smallrye.faulttolerance.reuse.sync.metrics.MyService.first"),
                new Tag("result", "valueReturned"),
                new Tag("fallback", "applied"))
                .getCount()).isEqualTo(1);

        assertThat(metrics.counter("ft.retry.retries.total",
                new Tag("method", "io.smallrye.faulttolerance.reuse.sync.metrics.MyService.first"))
                .getCount()).isEqualTo(2);
        assertThat(metrics.counter("ft.retry.calls.total",
                new Tag("method", "io.smallrye.faulttolerance.reuse.sync.metrics.MyService.first"),
                new Tag("retried", "true"),
                new Tag("retryResult", "maxRetriesReached"))
                .getCount()).isEqualTo(1);

        // second

        assertThat(metrics.counter("ft.invocations.total",
                new Tag("method", "io.smallrye.faulttolerance.reuse.sync.metrics.MyService.second"),
                new Tag("result", "valueReturned"),
                new Tag("fallback", "applied"))
                .getCount()).isEqualTo(3);

        assertThat(metrics.counter("ft.retry.retries.total",
                new Tag("method", "io.smallrye.faulttolerance.reuse.sync.metrics.MyService.second"))
                .getCount()).isEqualTo(6);
        assertThat(metrics.counter("ft.retry.calls.total",
                new Tag("method", "io.smallrye.faulttolerance.reuse.sync.metrics.MyService.second"),
                new Tag("retried", "true"),
                new Tag("retryResult", "maxRetriesReached"))
                .getCount()).isEqualTo(3);
    }
}
