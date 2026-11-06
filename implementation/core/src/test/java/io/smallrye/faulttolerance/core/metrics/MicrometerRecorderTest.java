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
import static io.smallrye.faulttolerance.core.metrics.MicrometerRecorder.BULKHEAD_RESULT_ACCEPTED;
import static io.smallrye.faulttolerance.core.metrics.MicrometerRecorder.CIRCUIT_BREAKER_RESULT_SUCCESS;
import static io.smallrye.faulttolerance.core.metrics.MicrometerRecorder.FALLBACK_APPLIED;
import static io.smallrye.faulttolerance.core.metrics.MicrometerRecorder.FALLBACK_NOT_APPLIED;
import static io.smallrye.faulttolerance.core.metrics.MicrometerRecorder.FALLBACK_NOT_DEFINED;
import static io.smallrye.faulttolerance.core.metrics.MicrometerRecorder.RATE_LIMIT_RESULT_PERMITTED;
import static io.smallrye.faulttolerance.core.metrics.MicrometerRecorder.RESULT_EXCEPTION_THROWN;
import static io.smallrye.faulttolerance.core.metrics.MicrometerRecorder.RESULT_VALUE_RETURNED;
import static io.smallrye.faulttolerance.core.metrics.MicrometerRecorder.RETRIED_FALSE;
import static io.smallrye.faulttolerance.core.metrics.MicrometerRecorder.RETRIED_TRUE;
import static io.smallrye.faulttolerance.core.metrics.MicrometerRecorder.RETRY_RESULT_EXCEPTION_NOT_RETRYABLE;
import static io.smallrye.faulttolerance.core.metrics.MicrometerRecorder.RETRY_RESULT_MAX_DURATION_REACHED;
import static io.smallrye.faulttolerance.core.metrics.MicrometerRecorder.RETRY_RESULT_MAX_RETRIES_REACHED;
import static io.smallrye.faulttolerance.core.metrics.MicrometerRecorder.RETRY_RESULT_VALUE_RETURNED;
import static io.smallrye.faulttolerance.core.metrics.MicrometerRecorder.TIMED_OUT_TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerEvents;

public class MicrometerRecorderTest {
    private static final Tag METHOD = Tag.of("method", "mock");

    private SimpleMeterRegistry registry;

    @BeforeEach
    public void setUp() {
        registry = new SimpleMeterRegistry();
    }

    @AfterEach
    public void tearDown() {
        registry.close();
    }

    private double invocations(Tag result, Tag fallback) {
        return registry.counter(MetricsConstants.INVOCATIONS_TOTAL, Tags.of(METHOD, result, fallback)).count();
    }

    @Test
    public void allInvocationCombinations_withoutFallbackInOperation() {
        MicrometerRecorder recorder = new MicrometerRecorder(registry, new MockMeteredOperation(false));

        recorder.executionFinished(true, true, true);
        recorder.executionFinished(true, true, false);
        recorder.executionFinished(true, false, false);
        recorder.executionFinished(false, true, true);
        recorder.executionFinished(false, true, false);
        recorder.executionFinished(false, false, false);

        assertThat(invocations(RESULT_VALUE_RETURNED, FALLBACK_APPLIED)).isEqualTo(1.0);
        assertThat(invocations(RESULT_VALUE_RETURNED, FALLBACK_NOT_APPLIED)).isEqualTo(1.0);
        assertThat(invocations(RESULT_VALUE_RETURNED, FALLBACK_NOT_DEFINED)).isEqualTo(1.0);
        assertThat(invocations(RESULT_EXCEPTION_THROWN, FALLBACK_APPLIED)).isEqualTo(1.0);
        assertThat(invocations(RESULT_EXCEPTION_THROWN, FALLBACK_NOT_APPLIED)).isEqualTo(1.0);
        assertThat(invocations(RESULT_EXCEPTION_THROWN, FALLBACK_NOT_DEFINED)).isEqualTo(1.0);
    }

    @Test
    public void allInvocationCombinations_withFallbackInOperation() {
        MicrometerRecorder recorder = new MicrometerRecorder(registry, new MockMeteredOperation(true));

        recorder.executionFinished(true, true, true);
        recorder.executionFinished(true, true, false);
        recorder.executionFinished(true, false, false);
        recorder.executionFinished(false, true, true);
        recorder.executionFinished(false, true, false);
        recorder.executionFinished(false, false, false);

        assertThat(invocations(RESULT_VALUE_RETURNED, FALLBACK_APPLIED)).isEqualTo(1.0);
        assertThat(invocations(RESULT_VALUE_RETURNED, FALLBACK_NOT_APPLIED)).isEqualTo(1.0);
        assertThat(invocations(RESULT_VALUE_RETURNED, FALLBACK_NOT_DEFINED)).isEqualTo(1.0);
        assertThat(invocations(RESULT_EXCEPTION_THROWN, FALLBACK_APPLIED)).isEqualTo(1.0);
        assertThat(invocations(RESULT_EXCEPTION_THROWN, FALLBACK_NOT_APPLIED)).isEqualTo(1.0);
        assertThat(invocations(RESULT_EXCEPTION_THROWN, FALLBACK_NOT_DEFINED)).isEqualTo(1.0);
    }

    @Test
    public void strategiesNotPresentInOperation() {
        MicrometerRecorder recorder = new MicrometerRecorder(registry, new MockMeteredOperation(false));

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

        assertThat(registry.counter(RETRY_RETRIES_TOTAL,
                Tags.of(METHOD)).count())
                .isEqualTo(1.0);
        assertThat(registry.counter(RETRY_CALLS_TOTAL,
                Tags.of(METHOD, RETRIED_TRUE, RETRY_RESULT_VALUE_RETURNED)).count())
                .isEqualTo(1.0);
        assertThat(registry.counter(RETRY_CALLS_TOTAL,
                Tags.of(METHOD, RETRIED_FALSE, RETRY_RESULT_EXCEPTION_NOT_RETRYABLE)).count())
                .isEqualTo(1.0);
        assertThat(registry.counter(RETRY_CALLS_TOTAL,
                Tags.of(METHOD, RETRIED_TRUE, RETRY_RESULT_MAX_RETRIES_REACHED)).count())
                .isEqualTo(1.0);
        assertThat(registry.counter(RETRY_CALLS_TOTAL,
                Tags.of(METHOD, RETRIED_FALSE, RETRY_RESULT_MAX_DURATION_REACHED)).count())
                .isEqualTo(1.0);

        assertThat(registry.counter(TIMEOUT_CALLS_TOTAL,
                Tags.of(METHOD, TIMED_OUT_TRUE)).count())
                .isEqualTo(1L);
        assertThat(registry.timer(TIMEOUT_EXECUTION_DURATION,
                Tags.of(METHOD)).count())
                .isEqualTo(1L);

        assertThat(registry.counter(CIRCUIT_BREAKER_CALLS_TOTAL,
                Tags.of(METHOD, CIRCUIT_BREAKER_RESULT_SUCCESS)).count())
                .isEqualTo(1.0);
        assertThat(registry.counter(CIRCUIT_BREAKER_OPENED_TOTAL,
                Tags.of(METHOD)).count())
                .isEqualTo(1.0);

        assertThat(registry.counter(BULKHEAD_CALLS_TOTAL,
                Tags.of(METHOD, BULKHEAD_RESULT_ACCEPTED)).count())
                .isEqualTo(1L);
        assertThat(registry.timer(BULKHEAD_RUNNING_DURATION,
                Tags.of(METHOD)).count())
                .isEqualTo(1L);
        assertThat(registry.timer(BULKHEAD_WAITING_DURATION,
                Tags.of(METHOD)).count())
                .isEqualTo(1L);

        assertThat(registry.counter(RATE_LIMIT_CALLS_TOTAL,
                Tags.of(METHOD, RATE_LIMIT_RESULT_PERMITTED)).count())
                .isEqualTo(1.0);
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
        public boolean hasCircuitBreaker() {
            return false;
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
        public boolean hasRetry() {
            return false;
        }

        @Override
        public boolean hasTimeout() {
            return false;
        }

        @Override
        public String name() {
            return "mock";
        }

        @Override
        public Object cacheKey() {
            return name();
        }
    }
}
