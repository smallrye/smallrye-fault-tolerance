package io.smallrye.faulttolerance.reuse.mixed.all.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.ExecutionException;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
public class MixedReuseAllMetricsTest {
    @Test
    public void test(MyService service, @RegistryType(type = MetricRegistry.Type.BASE) MetricRegistry metrics)
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

        assertThat(metrics.counter("ft.invocations.total",
                new Tag("method", "io.smallrye.faulttolerance.reuse.mixed.all.metrics.MyService.first"),
                new Tag("result", "valueReturned"),
                new Tag("fallback", "notDefined"))
                .getCount()).isEqualTo(1);

        assertThat(metrics.counter("ft.retry.retries.total",
                new Tag("method", "io.smallrye.faulttolerance.reuse.mixed.all.metrics.MyService.first"))
                .getCount()).isEqualTo(0);
        assertThat(metrics.counter("ft.retry.calls.total",
                new Tag("method", "io.smallrye.faulttolerance.reuse.mixed.all.metrics.MyService.first"),
                new Tag("retried", "false"),
                new Tag("retryResult", "valueReturned"))
                .getCount()).isEqualTo(1);

        // second

        assertThat(metrics.counter("ft.invocations.total",
                new Tag("method", "io.smallrye.faulttolerance.reuse.mixed.all.metrics.MyService.second"),
                new Tag("result", "valueReturned"),
                new Tag("fallback", "notDefined"))
                .getCount()).isEqualTo(2);

        assertThat(metrics.counter("ft.retry.retries.total",
                new Tag("method", "io.smallrye.faulttolerance.reuse.mixed.all.metrics.MyService.second"))
                .getCount()).isEqualTo(6);
        assertThat(metrics.counter("ft.retry.calls.total",
                new Tag("method", "io.smallrye.faulttolerance.reuse.mixed.all.metrics.MyService.second"),
                new Tag("retried", "true"),
                new Tag("retryResult", "valueReturned"))
                .getCount()).isEqualTo(2);

        // third

        assertThat(metrics.counter("ft.invocations.total",
                new Tag("method", "io.smallrye.faulttolerance.reuse.mixed.all.metrics.MyService.third"),
                new Tag("result", "exceptionThrown"),
                new Tag("fallback", "notDefined"))
                .getCount()).isEqualTo(3);

        assertThat(metrics.counter("ft.retry.retries.total",
                new Tag("method", "io.smallrye.faulttolerance.reuse.mixed.all.metrics.MyService.third"))
                .getCount()).isEqualTo(15);
        assertThat(metrics.counter("ft.retry.calls.total",
                new Tag("method", "io.smallrye.faulttolerance.reuse.mixed.all.metrics.MyService.third"),
                new Tag("retried", "true"),
                new Tag("retryResult", "maxRetriesReached"))
                .getCount()).isEqualTo(3);
    }
}
