package io.smallrye.faulttolerance.metrics.sameid;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.smallrye.faulttolerance.minimptel.MetricsAccess;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class SameMetricsIdTest {
    @Test
    public void test(MyService service, MetricsAccess metrics) {
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

        assertThat(metrics.get(LongPointData.class, "ft.invocations.total", Attributes.of(
                stringKey("method"), method,
                stringKey("result"), "valueReturned",
                stringKey("fallback"), "applied"))
                .getValue()).isEqualTo(1);

        assertThat(metrics.get(LongPointData.class, "ft.invocations.total", Attributes.of(
                stringKey("method"), method,
                stringKey("result"), "valueReturned",
                stringKey("fallback"), "notDefined"))
                .getValue()).isEqualTo(5);

        assertThat(metrics.getAll("ft.invocations.total")).hasSize(2);

        // The strategy-specific metrics don't collide, because each strategy is only used by
        // a single overload and hence carries a distinct set of tags.

        // retry
        assertThat(metrics.get(LongPointData.class, "ft.retry.retries.total", Attributes.of(
                stringKey("method"), method))
                .getValue()).isEqualTo(2);
        assertThat(metrics.get(LongPointData.class, "ft.retry.calls.total", Attributes.of(
                stringKey("method"), method,
                stringKey("retried"), "true",
                stringKey("retryResult"), "valueReturned"))
                .getValue()).isEqualTo(1);

        // circuit breaker
        assertThat(metrics.get(LongPointData.class, "ft.circuitbreaker.calls.total", Attributes.of(
                stringKey("method"), method,
                stringKey("circuitBreakerResult"), "success"))
                .getValue()).isEqualTo(1);

        // rate limit
        assertThat(metrics.get(LongPointData.class, "ft.ratelimit.calls.total", Attributes.of(
                stringKey("method"), method,
                stringKey("rateLimitResult"), "permitted"))
                .getValue()).isEqualTo(1);

        // timeout
        assertThat(metrics.get(LongPointData.class, "ft.timeout.calls.total", Attributes.of(
                stringKey("method"), method,
                stringKey("timedOut"), "false"))
                .getValue()).isEqualTo(1);

        // bulkhead
        assertThat(metrics.get(LongPointData.class, "ft.bulkhead.calls.total", Attributes.of(
                stringKey("method"), method,
                stringKey("bulkheadResult"), "accepted"))
                .getValue()).isEqualTo(1);
    }
}
