package io.smallrye.faulttolerance.reuse.mixed.all.metrics;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.ExecutionException;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.smallrye.faulttolerance.minimptel.MetricsAccess;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
public class MixedReuseAllMetricsTest {
    @Test
    public void test(MyService service, MetricsAccess metrics)
            throws ExecutionException, InterruptedException {
        // 1
        assertThat(service.first()).isEqualTo("hello");
        // 2
        assertThat(service.second().toCompletableFuture().get()).isEqualTo(42);
        service.resetSecondCounter();
        assertThat(service.second().toCompletableFuture().get()).isEqualTo(42);
        // 3
        assertThatCode(service.third().subscribeAsCompletionStage()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
        assertThatCode(service.third().subscribeAsCompletionStage()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
        assertThatCode(service.third().subscribeAsCompletionStage()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(IllegalArgumentException.class);

        // first

        assertThat(metrics.get(LongPointData.class, "ft.invocations.total", Attributes.of(
                stringKey("method"), "io.smallrye.faulttolerance.reuse.mixed.all.metrics.MyService.first",
                stringKey("result"), "valueReturned",
                stringKey("fallback"), "notDefined"))
                .getValue()).isEqualTo(1);

        assertThat(metrics.get(LongPointData.class, "ft.retry.retries.total", Attributes.of(
                stringKey("method"), "io.smallrye.faulttolerance.reuse.mixed.all.metrics.MyService.first")))
                // this metric would exist and be equal to 0 with Micrometer,
                // but we don't register all metrics eagerly with OpenTelemetry Metrics
                .isNull();
        assertThat(metrics.get(LongPointData.class, "ft.retry.calls.total", Attributes.of(
                stringKey("method"), "io.smallrye.faulttolerance.reuse.mixed.all.metrics.MyService.first",
                stringKey("retried"), "false",
                stringKey("retryResult"), "valueReturned"))
                .getValue()).isEqualTo(1);

        // second

        assertThat(metrics.get(LongPointData.class, "ft.invocations.total", Attributes.of(
                stringKey("method"), "io.smallrye.faulttolerance.reuse.mixed.all.metrics.MyService.second",
                stringKey("result"), "valueReturned",
                stringKey("fallback"), "notDefined"))
                .getValue()).isEqualTo(2);

        assertThat(metrics.get(LongPointData.class, "ft.retry.retries.total", Attributes.of(
                stringKey("method"), "io.smallrye.faulttolerance.reuse.mixed.all.metrics.MyService.second"))
                .getValue()).isEqualTo(6);
        assertThat(metrics.get(LongPointData.class, "ft.retry.calls.total", Attributes.of(
                stringKey("method"), "io.smallrye.faulttolerance.reuse.mixed.all.metrics.MyService.second",
                stringKey("retried"), "true",
                stringKey("retryResult"), "valueReturned"))
                .getValue()).isEqualTo(2);

        // third

        assertThat(metrics.get(LongPointData.class, "ft.invocations.total", Attributes.of(
                stringKey("method"), "io.smallrye.faulttolerance.reuse.mixed.all.metrics.MyService.third",
                stringKey("result"), "exceptionThrown",
                stringKey("fallback"), "notDefined"))
                .getValue()).isEqualTo(3);

        assertThat(metrics.get(LongPointData.class, "ft.retry.retries.total", Attributes.of(
                stringKey("method"), "io.smallrye.faulttolerance.reuse.mixed.all.metrics.MyService.third"))
                .getValue()).isEqualTo(15);
        assertThat(metrics.get(LongPointData.class, "ft.retry.calls.total", Attributes.of(
                stringKey("method"), "io.smallrye.faulttolerance.reuse.mixed.all.metrics.MyService.third",
                stringKey("retried"), "true",
                stringKey("retryResult"), "maxRetriesReached"))
                .getValue()).isEqualTo(3);
    }
}
