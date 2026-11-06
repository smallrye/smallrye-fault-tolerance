package io.smallrye.faulttolerance.core.metrics;

import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.BULKHEAD_CALLS_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.BULKHEAD_RUNNING_DURATION;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.BULKHEAD_WAITING_DURATION;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.CIRCUIT_BREAKER_CALLS_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.CIRCUIT_BREAKER_OPENED_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.RATE_LIMIT_CALLS_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.RETRY_CALLS_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.RETRY_RETRIES_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.TIMEOUT_CALLS_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.TIMEOUT_EXECUTION_DURATION;
import static io.smallrye.faulttolerance.core.metrics.OpenTelemetryRecorder.BULKHEAD_RESULT;
import static io.smallrye.faulttolerance.core.metrics.OpenTelemetryRecorder.BULKHEAD_RESULT_ACCEPTED;
import static io.smallrye.faulttolerance.core.metrics.OpenTelemetryRecorder.CIRCUIT_BREAKER_RESULT;
import static io.smallrye.faulttolerance.core.metrics.OpenTelemetryRecorder.CIRCUIT_BREAKER_RESULT_SUCCESS;
import static io.smallrye.faulttolerance.core.metrics.OpenTelemetryRecorder.FALLBACK_APPLIED;
import static io.smallrye.faulttolerance.core.metrics.OpenTelemetryRecorder.FALLBACK_NOT_APPLIED;
import static io.smallrye.faulttolerance.core.metrics.OpenTelemetryRecorder.FALLBACK_NOT_DEFINED;
import static io.smallrye.faulttolerance.core.metrics.OpenTelemetryRecorder.METHOD;
import static io.smallrye.faulttolerance.core.metrics.OpenTelemetryRecorder.RATE_LIMIT_RESULT;
import static io.smallrye.faulttolerance.core.metrics.OpenTelemetryRecorder.RATE_LIMIT_RESULT_PERMITTED;
import static io.smallrye.faulttolerance.core.metrics.OpenTelemetryRecorder.RESULT_EXCEPTION_THROWN;
import static io.smallrye.faulttolerance.core.metrics.OpenTelemetryRecorder.RESULT_VALUE_RETURNED;
import static io.smallrye.faulttolerance.core.metrics.OpenTelemetryRecorder.RETRIED;
import static io.smallrye.faulttolerance.core.metrics.OpenTelemetryRecorder.RETRIED_FALSE;
import static io.smallrye.faulttolerance.core.metrics.OpenTelemetryRecorder.RETRIED_TRUE;
import static io.smallrye.faulttolerance.core.metrics.OpenTelemetryRecorder.RETRY_RESULT;
import static io.smallrye.faulttolerance.core.metrics.OpenTelemetryRecorder.RETRY_RESULT_EXCEPTION_NOT_RETRYABLE;
import static io.smallrye.faulttolerance.core.metrics.OpenTelemetryRecorder.RETRY_RESULT_MAX_DURATION_REACHED;
import static io.smallrye.faulttolerance.core.metrics.OpenTelemetryRecorder.RETRY_RESULT_MAX_RETRIES_REACHED;
import static io.smallrye.faulttolerance.core.metrics.OpenTelemetryRecorder.RETRY_RESULT_VALUE_RETURNED;
import static io.smallrye.faulttolerance.core.metrics.OpenTelemetryRecorder.TIMED_OUT;
import static io.smallrye.faulttolerance.core.metrics.OpenTelemetryRecorder.TIMED_OUT_TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerEvents;

public class OpenTelemetryRecorderTest {
    private static final String METHOD_NAME = "mock";

    private InMemoryMetricReader reader;
    private SdkMeterProvider meterProvider;
    private Meter meter;

    @BeforeEach
    public void setUp() {
        reader = InMemoryMetricReader.create();
        meterProvider = SdkMeterProvider.builder().registerMetricReader(reader).build();
        meter = meterProvider.get("test");
    }

    @AfterEach
    public void tearDown() {
        meterProvider.close();
    }

    private long counter(String name, Attributes attributes) {
        return reader.collectAllMetrics()
                .stream()
                .filter(metric -> metric.getName().equals(name))
                .flatMap(metric -> metric.getLongSumData().getPoints().stream())
                .filter(point -> point.getAttributes().equals(attributes))
                .mapToLong(LongPointData::getValue)
                .sum();
    }

    private long histogramCount(String name, Attributes attributes) {
        return reader.collectAllMetrics()
                .stream()
                .filter(metric -> metric.getName().equals(name))
                .flatMap(metric -> metric.getHistogramData().getPoints().stream())
                .filter(point -> point.getAttributes().equals(attributes))
                .mapToLong(HistogramPointData::getCount)
                .sum();
    }

    private long invocations(String result, String fallback) {
        return counter(MetricsConstants.INVOCATIONS_TOTAL, Attributes.of(
                METHOD, METHOD_NAME,
                OpenTelemetryRecorder.RESULT, result,
                OpenTelemetryRecorder.FALLBACK, fallback));
    }

    @Test
    public void allInvocationCombinations_withoutFallbackInOperation() {
        OpenTelemetryRecorder recorder = new OpenTelemetryRecorder(meter, new MockMeteredOperation(false));

        recorder.executionFinished(true, true, true, 0);
        recorder.executionFinished(true, true, false, 0);
        recorder.executionFinished(true, false, false, 0);
        recorder.executionFinished(false, true, true, 0);
        recorder.executionFinished(false, true, false, 0);
        recorder.executionFinished(false, false, false, 0);

        assertThat(invocations(RESULT_VALUE_RETURNED, FALLBACK_APPLIED)).isEqualTo(1);
        assertThat(invocations(RESULT_VALUE_RETURNED, FALLBACK_NOT_APPLIED)).isEqualTo(1);
        assertThat(invocations(RESULT_VALUE_RETURNED, FALLBACK_NOT_DEFINED)).isEqualTo(1);
        assertThat(invocations(RESULT_EXCEPTION_THROWN, FALLBACK_APPLIED)).isEqualTo(1);
        assertThat(invocations(RESULT_EXCEPTION_THROWN, FALLBACK_NOT_APPLIED)).isEqualTo(1);
        assertThat(invocations(RESULT_EXCEPTION_THROWN, FALLBACK_NOT_DEFINED)).isEqualTo(1);
    }

    @Test
    public void allInvocationCombinations_withFallbackInOperation() {
        OpenTelemetryRecorder recorder = new OpenTelemetryRecorder(meter, new MockMeteredOperation(true));

        recorder.executionFinished(true, true, true, 0);
        recorder.executionFinished(true, true, false, 0);
        recorder.executionFinished(true, false, false, 0);
        recorder.executionFinished(false, true, true, 0);
        recorder.executionFinished(false, true, false, 0);
        recorder.executionFinished(false, false, false, 0);

        assertThat(invocations(RESULT_VALUE_RETURNED, FALLBACK_APPLIED)).isEqualTo(1);
        assertThat(invocations(RESULT_VALUE_RETURNED, FALLBACK_NOT_APPLIED)).isEqualTo(1);
        assertThat(invocations(RESULT_VALUE_RETURNED, FALLBACK_NOT_DEFINED)).isEqualTo(1);
        assertThat(invocations(RESULT_EXCEPTION_THROWN, FALLBACK_APPLIED)).isEqualTo(1);
        assertThat(invocations(RESULT_EXCEPTION_THROWN, FALLBACK_NOT_APPLIED)).isEqualTo(1);
        assertThat(invocations(RESULT_EXCEPTION_THROWN, FALLBACK_NOT_DEFINED)).isEqualTo(1);
    }

    @Test
    public void strategiesNotPresentInOperation() {
        OpenTelemetryRecorder recorder = new OpenTelemetryRecorder(meter, new MockMeteredOperation(false));

        assertThatCode(() -> recorder.retryAttempted()).doesNotThrowAnyException();
        assertThatCode(() -> recorder.retryValueReturned(true)).doesNotThrowAnyException();
        assertThatCode(() -> recorder.retryExceptionNotRetryable(false)).doesNotThrowAnyException();
        assertThatCode(() -> recorder.retryMaxRetriesReached(true)).doesNotThrowAnyException();
        assertThatCode(() -> recorder.retryMaxDurationReached(false)).doesNotThrowAnyException();
        assertThatCode(() -> recorder.timeoutFinished(true, 1_000_000L)).doesNotThrowAnyException();
        assertThatCode(() -> recorder.circuitBreakerFinished(CircuitBreakerEvents.Result.SUCCESS)).doesNotThrowAnyException();
        assertThatCode(() -> recorder.circuitBreakerMovedToOpen()).doesNotThrowAnyException();
        assertThatCode(() -> recorder.bulkheadDecisionMade(true)).doesNotThrowAnyException();
        assertThatCode(() -> recorder.updateBulkheadRunningDuration(1_000_000L)).doesNotThrowAnyException();
        assertThatCode(() -> recorder.updateBulkheadWaitingDuration(1_000_000L)).doesNotThrowAnyException();
        assertThatCode(() -> recorder.rateLimitDecisionMade(true)).doesNotThrowAnyException();

        assertThat(counter(RETRY_RETRIES_TOTAL,
                Attributes.of(METHOD, METHOD_NAME)))
                .isEqualTo(1);
        assertThat(counter(RETRY_CALLS_TOTAL,
                Attributes.of(METHOD, METHOD_NAME, RETRIED, RETRIED_TRUE, RETRY_RESULT, RETRY_RESULT_VALUE_RETURNED)))
                .isEqualTo(1);
        assertThat(counter(RETRY_CALLS_TOTAL,
                Attributes.of(METHOD, METHOD_NAME, RETRIED, RETRIED_FALSE, RETRY_RESULT, RETRY_RESULT_EXCEPTION_NOT_RETRYABLE)))
                .isEqualTo(1);
        assertThat(counter(RETRY_CALLS_TOTAL,
                Attributes.of(METHOD, METHOD_NAME, RETRIED, RETRIED_TRUE, RETRY_RESULT, RETRY_RESULT_MAX_RETRIES_REACHED)))
                .isEqualTo(1);
        assertThat(counter(RETRY_CALLS_TOTAL,
                Attributes.of(METHOD, METHOD_NAME, RETRIED, RETRIED_FALSE, RETRY_RESULT, RETRY_RESULT_MAX_DURATION_REACHED)))
                .isEqualTo(1);

        assertThat(counter(TIMEOUT_CALLS_TOTAL,
                Attributes.of(METHOD, METHOD_NAME, TIMED_OUT, TIMED_OUT_TRUE)))
                .isEqualTo(1);
        assertThat(histogramCount(TIMEOUT_EXECUTION_DURATION,
                Attributes.of(METHOD, METHOD_NAME)))
                .isEqualTo(1);

        assertThat(counter(CIRCUIT_BREAKER_CALLS_TOTAL,
                Attributes.of(METHOD, METHOD_NAME, CIRCUIT_BREAKER_RESULT, CIRCUIT_BREAKER_RESULT_SUCCESS)))
                .isEqualTo(1);
        assertThat(counter(CIRCUIT_BREAKER_OPENED_TOTAL,
                Attributes.of(METHOD, METHOD_NAME)))
                .isEqualTo(1);

        assertThat(counter(BULKHEAD_CALLS_TOTAL,
                Attributes.of(METHOD, METHOD_NAME, BULKHEAD_RESULT, BULKHEAD_RESULT_ACCEPTED)))
                .isEqualTo(1);
        assertThat(histogramCount(BULKHEAD_RUNNING_DURATION,
                Attributes.of(METHOD, METHOD_NAME)))
                .isEqualTo(1);
        assertThat(histogramCount(BULKHEAD_WAITING_DURATION,
                Attributes.of(METHOD, METHOD_NAME)))
                .isEqualTo(1);

        assertThat(counter(RATE_LIMIT_CALLS_TOTAL,
                Attributes.of(METHOD, METHOD_NAME, RATE_LIMIT_RESULT, RATE_LIMIT_RESULT_PERMITTED)))
                .isEqualTo(1);
    }

    private static final class MockMeteredOperation implements MeteredOperation {
        private final boolean hasFallback;

        MockMeteredOperation(boolean hasFallback) {
            this.hasFallback = hasFallback;
        }

        @Override
        public boolean enabled() {
            return true;
        }

        @Override
        public boolean mayBeAsynchronous() {
            return false;
        }

        @Override
        public boolean hasBulkhead() {
            return false;
        }

        @Override
        public int bulkheadLimit() {
            return 0;
        }

        @Override
        public int bulkheadQueueSize() {
            return 0;
        }

        @Override
        public boolean hasCircuitBreaker() {
            return false;
        }

        @Override
        public long circuitBreakerDelay() {
            return 0;
        }

        @Override
        public int circuitBreakerRequestVolumeThreshold() {
            return 0;
        }

        @Override
        public double circuitBreakerFailureRatio() {
            return 0;
        }

        @Override
        public int circuitBreakerSuccessThreshold() {
            return 0;
        }

        @Override
        public boolean hasFallback() {
            return hasFallback;
        }

        @Override
        public boolean hasRateLimit() {
            return false;
        }

        @Override
        public int rateLimitLimit() {
            return 0;
        }

        @Override
        public long rateLimitWindow() {
            return 0;
        }

        @Override
        public long rateLimitMinSpacing() {
            return 0;
        }

        @Override
        public boolean hasRetry() {
            return false;
        }

        @Override
        public int retryMaxRetries() {
            return 0;
        }

        @Override
        public long retryMaxDuration() {
            return 0;
        }

        @Override
        public boolean hasTimeout() {
            return false;
        }

        @Override
        public long timeoutDuration() {
            return 0;
        }

        @Override
        public String name() {
            return METHOD_NAME;
        }

        @Override
        public Object cacheKey() {
            return name();
        }
    }
}
