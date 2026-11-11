package io.smallrye.faulttolerance.metrics.sameid;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class SameMetricsIdTest {
    @Test
    public void test(MyService service, @RegistryType(type = MetricRegistry.Type.BASE) MetricRegistry metrics) {
        MyService.FALLBACK_COUNTER.set(0);
        MyService.RETRY_COUNTER.set(0);
        MyService.CIRCUIT_BREAKER_COUNTER.set(0);
        MyService.RATE_LIMIT_COUNTER.set(0);
        MyService.TIMEOUT_COUNTER.set(0);
        MyService.BULKHEAD_COUNTER.set(0);

        assertThat(service.hello()).isEqualTo("hello");
        assertThat(service.hello(0)).isEqualTo("hello");
        assertThat(service.hello("")).isEqualTo("hello");
        assertThat(service.hello(List.of())).isEqualTo("hello");
        assertThat(service.hello(Set.of())).isEqualTo("hello");
        assertThat(service.hello(Map.of())).isEqualTo("hello");

        assertThat(MyService.FALLBACK_COUNTER.get()).isEqualTo(1);
        assertThat(MyService.RETRY_COUNTER.get()).isEqualTo(3);
        assertThat(MyService.CIRCUIT_BREAKER_COUNTER.get()).isEqualTo(1);
        assertThat(MyService.RATE_LIMIT_COUNTER.get()).isEqualTo(1);
        assertThat(MyService.TIMEOUT_COUNTER.get()).isEqualTo(1);
        assertThat(MyService.BULKHEAD_COUNTER.get()).isEqualTo(1);

        String method = MyService.class.getName() + ".hello";

        // Even though the six overloads use six different strategies, they all report the same
        // `method` attribute, so the general `ft.invocations.total` metric collapses into a single
        // series per [result, fallback] tag combination. Only the fallback overload differs by
        // having `fallback=applied`; the other five succeed with `fallback=notDefined` and merge.

        assertThat(metrics.counter("ft.invocations.total",
                new Tag("method", method),
                new Tag("result", "valueReturned"),
                new Tag("fallback", "applied"))
                .getCount()).isEqualTo(1);

        assertThat(metrics.counter("ft.invocations.total",
                new Tag("method", method),
                new Tag("result", "valueReturned"),
                new Tag("fallback", "notDefined"))
                .getCount()).isEqualTo(5);

        assertThat(metrics.getCounters((id, metric) -> "ft.invocations.total".equals(id.getName())).values())
                .filteredOn(c -> c.getCount() > 0)
                .hasSize(2);

        // The strategy-specific metrics don't collide, because each strategy is only used by
        // a single overload and hence carries a distinct set of tags.

        // retry
        assertThat(metrics.counter("ft.retry.retries.total",
                new Tag("method", method))
                .getCount()).isEqualTo(2);
        assertThat(metrics.counter("ft.retry.calls.total",
                new Tag("method", method),
                new Tag("retried", "true"),
                new Tag("retryResult", "valueReturned"))
                .getCount()).isEqualTo(1);

        // circuit breaker
        assertThat(metrics.counter("ft.circuitbreaker.calls.total",
                new Tag("method", method),
                new Tag("circuitBreakerResult", "success"))
                .getCount()).isEqualTo(1);

        // rate limit
        assertThat(metrics.counter("ft.ratelimit.calls.total",
                new Tag("method", method),
                new Tag("rateLimitResult", "permitted"))
                .getCount()).isEqualTo(1);

        // timeout
        assertThat(metrics.counter("ft.timeout.calls.total",
                new Tag("method", method),
                new Tag("timedOut", "false"))
                .getCount()).isEqualTo(1);

        // bulkhead
        assertThat(metrics.counter("ft.bulkhead.calls.total",
                new Tag("method", method),
                new Tag("bulkheadResult", "accepted"))
                .getCount()).isEqualTo(1);
    }
}
