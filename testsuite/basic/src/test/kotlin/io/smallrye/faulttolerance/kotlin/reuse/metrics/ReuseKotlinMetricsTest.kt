package io.smallrye.faulttolerance.kotlin.reuse.metrics

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.metrics.data.LongPointData
import io.smallrye.faulttolerance.minimptel.MetricsAccess
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.jboss.weld.junit5.auto.AddBeanClasses
import org.junit.jupiter.api.Test

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance::class)
class ReuseKotlinMetricsTest {
    @Test
    fun test(service: MyService, metrics: MetricsAccess) = runBlocking<Unit> {
        assertThat(service.first()).isEqualTo("fallback")
        assertThat(service.second()).isEqualTo("fallback")
        assertThat(service.second()).isEqualTo("fallback")
        assertThat(service.second()).isEqualTo("fallback")

        // first

        assertThat(metrics.get(LongPointData::class.java, "ft.invocations.total", Attributes.of(
            stringKey("method"), "io.smallrye.faulttolerance.kotlin.reuse.metrics.MyService.first",
            stringKey("result"), "valueReturned",
            stringKey("fallback"), "applied")
        ).value).isEqualTo(1);

        assertThat(metrics.get(LongPointData::class.java, "ft.retry.retries.total", Attributes.of(
            stringKey("method"), "io.smallrye.faulttolerance.kotlin.reuse.metrics.MyService.first"))
            .value).isEqualTo(2);
        assertThat(metrics.get(LongPointData::class.java, "ft.retry.calls.total", Attributes.of(
            stringKey("method"), "io.smallrye.faulttolerance.kotlin.reuse.metrics.MyService.first",
            stringKey("retried"), "true",
            stringKey("retryResult"), "maxRetriesReached")
        ).value).isEqualTo(1);

        // second

        assertThat(metrics.get(LongPointData::class.java, "ft.invocations.total", Attributes.of(
            stringKey("method"), "io.smallrye.faulttolerance.kotlin.reuse.metrics.MyService.second",
            stringKey("result"), "valueReturned",
            stringKey("fallback"), "applied")
        ).value).isEqualTo(3);

        assertThat(metrics.get(LongPointData::class.java, "ft.retry.retries.total", Attributes.of(
            stringKey("method"), "io.smallrye.faulttolerance.kotlin.reuse.metrics.MyService.second")
        ).value).isEqualTo(6);
        assertThat(metrics.get(LongPointData::class.java, "ft.retry.calls.total", Attributes.of(
            stringKey("method"), "io.smallrye.faulttolerance.kotlin.reuse.metrics.MyService.second",
            stringKey("retried"), "true",
            stringKey("retryResult"), "maxRetriesReached")
        ).value).isEqualTo(3);
    }
}
