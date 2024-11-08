package io.smallrye.faulttolerance.programmatic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Callable;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.core.metrics.MetricsConstants;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

// needs to stay in sync with `StandaloneMetricsTest`
@FaultToleranceBasicTest
public class CdiMetricsTest {
    private static final String NAME = CdiMetricsTest.class.getName() + " programmatic usage";

    @Test
    public void test(@RegistryType(type = MetricRegistry.Type.BASE) MetricRegistry metrics) throws Exception {
        Callable<String> guarded = TypedGuard.create(String.class)
                .withDescription(NAME)
                .withFallback().handler(this::fallback).done()
                .withRetry().maxRetries(3).done()
                .build()
                .adaptCallable(this::action);

        assertThat(guarded.call()).isEqualTo("fallback");

        assertThat(metrics.counter(MetricsConstants.INVOCATIONS_TOTAL,
                new Tag("method", NAME),
                new Tag("result", "valueReturned"),
                new Tag("fallback", "applied"))
                .getCount()).isEqualTo(1);

        assertThat(metrics.counter(MetricsConstants.RETRY_RETRIES_TOTAL,
                new Tag("method", NAME))
                .getCount()).isEqualTo(3);
        assertThat(metrics.counter(MetricsConstants.RETRY_CALLS_TOTAL,
                new Tag("method", NAME),
                new Tag("retried", "true"),
                new Tag("retryResult", "maxRetriesReached"))
                .getCount()).isEqualTo(1);
    }

    public String action() throws TestException {
        throw new TestException();
    }

    public String fallback() {
        return "fallback";
    }
}
