package io.smallrye.faulttolerance.programmatic;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.core.metrics.MetricsConstants;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.minimptel.MetricsAccess;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

// needs to stay in sync with `StandaloneMetricsTest`
@FaultToleranceBasicTest
public class CdiMetricsTest {
    private static final String NAME = CdiMetricsTest.class.getName() + " programmatic usage";

    @Test
    public void metricsWithDescription(MetricsAccess metrics) throws Exception {
        int oldSize = metrics.getAll(MetricsConstants.INVOCATIONS_TOTAL).size();

        Callable<String> guarded = TypedGuard.create(String.class)
                .withDescription(NAME)
                .withFallback().handler(this::fallback).done()
                .withRetry().maxRetries(3).done()
                .build()
                .adaptCallable(this::action);

        assertThat(guarded.call()).isEqualTo("fallback");

        assertThat(metrics.getAll(MetricsConstants.INVOCATIONS_TOTAL).size()).isGreaterThan(oldSize);

        assertThat(metrics.get(LongPointData.class, MetricsConstants.INVOCATIONS_TOTAL, Attributes.of(
                stringKey("method"), NAME,
                stringKey("result"), "valueReturned",
                stringKey("fallback"), "applied"))
                .getValue()).isEqualTo(1);

        assertThat(metrics.get(LongPointData.class, MetricsConstants.RETRY_RETRIES_TOTAL, Attributes.of(
                stringKey("method"), NAME))
                .getValue()).isEqualTo(3);
        assertThat(metrics.get(LongPointData.class, MetricsConstants.RETRY_CALLS_TOTAL, Attributes.of(
                stringKey("method"), NAME,
                stringKey("retried"), "true",
                stringKey("retryResult"), "maxRetriesReached"))
                .getValue()).isEqualTo(1);
    }

    @Test
    public void noMetricsWithoutDescription(MetricsAccess metrics)
            throws Exception {
        int oldSize = metrics.getAll(MetricsConstants.INVOCATIONS_TOTAL).size();

        Callable<String> guarded = TypedGuard.create(String.class)
                .withFallback().handler(this::fallback).done()
                .withRetry().maxRetries(3).done()
                .build()
                .adaptCallable(this::action);

        assertThat(guarded.call()).isEqualTo("fallback");

        assertThat(metrics.getAll(MetricsConstants.INVOCATIONS_TOTAL).size()).isEqualTo(oldSize);
    }

    public String action() throws TestException {
        throw new TestException();
    }

    public String fallback() {
        return "fallback";
    }
}
