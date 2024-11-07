package io.smallrye.faulttolerance.reuse.async.completionstage.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutionException;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
public class ReuseAsyncCompletionStageMetricsTest {
    @Test
    public void test(MyService service, @RegistryType(type = MetricRegistry.Type.BASE) MetricRegistry metrics)
            throws ExecutionException, InterruptedException {
        assertThat(service.first().toCompletableFuture().get()).isEqualTo("fallback");
        assertThat(service.second().toCompletableFuture().get()).isEqualTo("fallback");
        assertThat(service.second().toCompletableFuture().get()).isEqualTo("fallback");
        assertThat(service.second().toCompletableFuture().get()).isEqualTo("fallback");

        // first

        assertThat(metrics.counter("ft.invocations.total",
                new Tag("method", "io.smallrye.faulttolerance.reuse.async.completionstage.metrics.MyService.first"),
                new Tag("result", "valueReturned"),
                new Tag("fallback", "applied"))
                .getCount()).isEqualTo(1);

        assertThat(metrics.counter("ft.retry.retries.total",
                new Tag("method", "io.smallrye.faulttolerance.reuse.async.completionstage.metrics.MyService.first"))
                .getCount()).isEqualTo(2);
        assertThat(metrics.counter("ft.retry.calls.total",
                new Tag("method", "io.smallrye.faulttolerance.reuse.async.completionstage.metrics.MyService.first"),
                new Tag("retried", "true"),
                new Tag("retryResult", "maxRetriesReached"))
                .getCount()).isEqualTo(1);

        // second

        assertThat(metrics.counter("ft.invocations.total",
                new Tag("method", "io.smallrye.faulttolerance.reuse.async.completionstage.metrics.MyService.second"),
                new Tag("result", "valueReturned"),
                new Tag("fallback", "applied"))
                .getCount()).isEqualTo(3);

        assertThat(metrics.counter("ft.retry.retries.total",
                new Tag("method", "io.smallrye.faulttolerance.reuse.async.completionstage.metrics.MyService.second"))
                .getCount()).isEqualTo(6);
        assertThat(metrics.counter("ft.retry.calls.total",
                new Tag("method", "io.smallrye.faulttolerance.reuse.async.completionstage.metrics.MyService.second"),
                new Tag("retried", "true"),
                new Tag("retryResult", "maxRetriesReached"))
                .getCount()).isEqualTo(3);
    }
}