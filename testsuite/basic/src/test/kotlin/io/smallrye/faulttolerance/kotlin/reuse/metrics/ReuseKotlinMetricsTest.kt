package io.smallrye.faulttolerance.kotlin.reuse.metrics

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.microprofile.metrics.MetricRegistry
import org.eclipse.microprofile.metrics.Tag
import org.eclipse.microprofile.metrics.annotation.RegistryType
import org.jboss.weld.junit5.auto.AddBeanClasses
import org.junit.jupiter.api.Test

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance::class)
class ReuseKotlinMetricsTest {
    @Test
    fun test(service: MyService, @RegistryType(type = MetricRegistry.Type.BASE) metrics: MetricRegistry) = runBlocking<Unit> {
        assertThat(service.first()).isEqualTo("fallback")
        assertThat(service.second()).isEqualTo("fallback")
        assertThat(service.second()).isEqualTo("fallback")
        assertThat(service.second()).isEqualTo("fallback")

        // first

        assertThat(metrics.counter("ft.invocations.total",
            Tag("method", "io.smallrye.faulttolerance.kotlin.reuse.metrics.MyService.first"),
            Tag("result", "valueReturned"),
            Tag("fallback", "applied")
        ).count).isEqualTo(1);

        assertThat(metrics.counter("ft.retry.retries.total",
            Tag("method", "io.smallrye.faulttolerance.kotlin.reuse.metrics.MyService.first"))
            .count).isEqualTo(2);
        assertThat(metrics.counter("ft.retry.calls.total",
            Tag("method", "io.smallrye.faulttolerance.kotlin.reuse.metrics.MyService.first"),
            Tag("retried", "true"),
            Tag("retryResult", "maxRetriesReached")
        ).count).isEqualTo(1);

        // second

        assertThat(metrics.counter("ft.invocations.total",
            Tag("method", "io.smallrye.faulttolerance.kotlin.reuse.metrics.MyService.second"),
            Tag("result", "valueReturned"),
            Tag("fallback", "applied")
        ).count).isEqualTo(3);

        assertThat(metrics.counter("ft.retry.retries.total",
            Tag("method", "io.smallrye.faulttolerance.kotlin.reuse.metrics.MyService.second")
        ).count).isEqualTo(6);
        assertThat(metrics.counter("ft.retry.calls.total",
            Tag("method", "io.smallrye.faulttolerance.kotlin.reuse.metrics.MyService.second"),
            Tag("retried", "true"),
            Tag("retryResult", "maxRetriesReached")
        ).count).isEqualTo(3);
    }
}
