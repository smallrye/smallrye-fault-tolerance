package io.smallrye.faulttolerance.metrics;

import static io.smallrye.faulttolerance.metrics.MetricConstants.BULKHEAD_CALLS_TOTAL;
import static io.smallrye.faulttolerance.metrics.MetricConstants.BULKHEAD_EXECUTIONS_RUNNING;
import static io.smallrye.faulttolerance.metrics.MetricConstants.BULKHEAD_EXECUTIONS_WAITING;
import static io.smallrye.faulttolerance.metrics.MetricConstants.BULKHEAD_RUNNING_DURATION;
import static io.smallrye.faulttolerance.metrics.MetricConstants.BULKHEAD_WAITING_DURATION;
import static io.smallrye.faulttolerance.metrics.MetricConstants.CIRCUIT_BREAKER_CALLS_TOTAL;
import static io.smallrye.faulttolerance.metrics.MetricConstants.CIRCUIT_BREAKER_OPENED_TOTAL;
import static io.smallrye.faulttolerance.metrics.MetricConstants.CIRCUIT_BREAKER_STATE_CURRENT;
import static io.smallrye.faulttolerance.metrics.MetricConstants.CIRCUIT_BREAKER_STATE_TOTAL;
import static io.smallrye.faulttolerance.metrics.MetricConstants.INVOCATIONS_TOTAL;
import static io.smallrye.faulttolerance.metrics.MetricConstants.RATE_LIMIT_CALLS_TOTAL;
import static io.smallrye.faulttolerance.metrics.MetricConstants.RETRY_CALLS_TOTAL;
import static io.smallrye.faulttolerance.metrics.MetricConstants.RETRY_RETRIES_TOTAL;
import static io.smallrye.faulttolerance.metrics.MetricConstants.TIMEOUT_CALLS_TOTAL;
import static io.smallrye.faulttolerance.metrics.MetricConstants.TIMEOUT_EXECUTION_DURATION;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.smallrye.faulttolerance.SpecCompatibility;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerEvents;
import io.smallrye.faulttolerance.core.metrics.MetricsRecorder;

@Singleton
public class MicrometerProvider implements MetricsProvider {
    static final Tag RESULT_VALUE_RETURNED = Tag.of("result", "valueReturned");
    static final Tag RESULT_EXCEPTION_THROWN = Tag.of("result", "exceptionThrown");

    static final Tag FALLBACK_APPLIED = Tag.of("fallback", "applied");
    static final Tag FALLBACK_NOT_APPLIED = Tag.of("fallback", "notApplied");
    static final Tag FALLBACK_NOT_DEFINED = Tag.of("fallback", "notDefined");

    static final Tag RETRIED_TRUE = Tag.of("retried", "true");
    static final Tag RETRIED_FALSE = Tag.of("retried", "false");
    static final Tag RETRY_RESULT_VALUE_RETURNED = Tag.of("retryResult", "valueReturned");
    static final Tag RETRY_RESULT_EXCEPTION_NOT_RETRYABLE = Tag.of("retryResult", "exceptionNotRetryable");
    static final Tag RETRY_RESULT_MAX_RETRIES_REACHED = Tag.of("retryResult", "maxRetriesReached");
    static final Tag RETRY_RESULT_MAX_DURATION_REACHED = Tag.of("retryResult", "maxDurationReached");

    static final Tag TIMED_OUT_TRUE = Tag.of("timedOut", "true");
    static final Tag TIMED_OUT_FALSE = Tag.of("timedOut", "false");

    static final Tag CIRCUIT_BREAKER_RESULT_SUCCESS = Tag.of("circuitBreakerResult", "success");
    static final Tag CIRCUIT_BREAKER_RESULT_FAILURE = Tag.of("circuitBreakerResult", "failure");
    static final Tag CIRCUIT_BREAKER_RESULT_CB_OPEN = Tag.of("circuitBreakerResult", "circuitBreakerOpen");

    static final Tag CIRCUIT_BREAKER_STATE_CLOSED = Tag.of("state", "closed");
    static final Tag CIRCUIT_BREAKER_STATE_OPEN = Tag.of("state", "open");
    static final Tag CIRCUIT_BREAKER_STATE_HALF_OPEN = Tag.of("state", "halfOpen");

    static final Tag BULKHEAD_RESULT_ACCEPTED = Tag.of("bulkheadResult", "accepted");
    static final Tag BULKHEAD_RESULT_REJECTED = Tag.of("bulkheadResult", "rejected");

    static final Tag RATE_LIMIT_RESULT_PERMITTED = Tag.of("rateLimitResult", "permitted");
    static final Tag RATE_LIMIT_RESULT_REJECTED = Tag.of("rateLimitResult", "rejected");

    @Inject
    MeterRegistry registry;

    @Inject
    @ConfigProperty(name = "MP_Fault_Tolerance_Metrics_Enabled", defaultValue = "true")
    boolean metricsEnabled;

    @Inject
    SpecCompatibility specCompatibility;

    @Override
    public MetricsRecorder create(FaultToleranceOperation operation) {
        if (metricsEnabled) {
            return new MetricsRecorderImpl(registry, specCompatibility, operation);
        } else {
            return MetricsRecorder.NOOP;
        }
    }

    @Override
    public boolean isEnabled() {
        return metricsEnabled;
    }

    private static class MetricsRecorderImpl implements MetricsRecorder {
        private final MeterRegistry registry;
        private final Tag methodTag;
        private final Iterable<Tag> methodTagSingleton;

        MetricsRecorderImpl(MeterRegistry registry, SpecCompatibility specCompatibility, FaultToleranceOperation operation) {
            this.registry = registry;
            this.methodTag = Tag.of("method", operation.getName());
            this.methodTagSingleton = Collections.singleton(methodTag);

            registerMetrics(specCompatibility, operation);
        }

        private void registerMetrics(SpecCompatibility specCompatibility, FaultToleranceOperation operation) {
            // make sure all applicable metrics for given method are registered eagerly
            // we only touch counters and histograms, because gauges are registered eagerly otherwise

            if (operation.hasFallback()) {
                registry.counter(INVOCATIONS_TOTAL, Arrays.asList(methodTag, RESULT_VALUE_RETURNED, FALLBACK_NOT_APPLIED));
                registry.counter(INVOCATIONS_TOTAL, Arrays.asList(methodTag, RESULT_VALUE_RETURNED, FALLBACK_APPLIED)).count();
                registry.counter(INVOCATIONS_TOTAL, Arrays.asList(methodTag, RESULT_EXCEPTION_THROWN, FALLBACK_NOT_APPLIED));
                registry.counter(INVOCATIONS_TOTAL, Arrays.asList(methodTag, RESULT_EXCEPTION_THROWN, FALLBACK_APPLIED));
            } else {
                registry.counter(INVOCATIONS_TOTAL, Arrays.asList(methodTag, RESULT_VALUE_RETURNED, FALLBACK_NOT_DEFINED));
                registry.counter(INVOCATIONS_TOTAL, Arrays.asList(methodTag, RESULT_EXCEPTION_THROWN, FALLBACK_NOT_DEFINED));
            }

            if (operation.hasRetry()) {
                registry.counter(RETRY_RETRIES_TOTAL, methodTagSingleton);

                registry.counter(RETRY_CALLS_TOTAL, Arrays.asList(methodTag, RETRIED_FALSE, RETRY_RESULT_VALUE_RETURNED));
                registry.counter(RETRY_CALLS_TOTAL,
                        Arrays.asList(methodTag, RETRIED_FALSE, RETRY_RESULT_EXCEPTION_NOT_RETRYABLE));
                registry.counter(RETRY_CALLS_TOTAL, Arrays.asList(methodTag, RETRIED_FALSE, RETRY_RESULT_MAX_RETRIES_REACHED));
                registry.counter(RETRY_CALLS_TOTAL, Arrays.asList(methodTag, RETRIED_FALSE, RETRY_RESULT_MAX_DURATION_REACHED));
                registry.counter(RETRY_CALLS_TOTAL, Arrays.asList(methodTag, RETRIED_TRUE, RETRY_RESULT_VALUE_RETURNED));
                registry.counter(RETRY_CALLS_TOTAL,
                        Arrays.asList(methodTag, RETRIED_TRUE, RETRY_RESULT_EXCEPTION_NOT_RETRYABLE));
                registry.counter(RETRY_CALLS_TOTAL, Arrays.asList(methodTag, RETRIED_TRUE, RETRY_RESULT_MAX_RETRIES_REACHED));
                registry.counter(RETRY_CALLS_TOTAL, Arrays.asList(methodTag, RETRIED_TRUE, RETRY_RESULT_MAX_DURATION_REACHED));
            }

            if (operation.hasTimeout()) {
                registry.counter(TIMEOUT_CALLS_TOTAL, Arrays.asList(methodTag, TIMED_OUT_TRUE));
                registry.counter(TIMEOUT_CALLS_TOTAL, Arrays.asList(methodTag, TIMED_OUT_FALSE));

                registry.timer(TIMEOUT_EXECUTION_DURATION, methodTagSingleton);
            }

            if (operation.hasCircuitBreaker()) {
                registry.counter(CIRCUIT_BREAKER_CALLS_TOTAL, Arrays.asList(methodTag, CIRCUIT_BREAKER_RESULT_SUCCESS));
                registry.counter(CIRCUIT_BREAKER_CALLS_TOTAL, Arrays.asList(methodTag, CIRCUIT_BREAKER_RESULT_FAILURE));
                registry.counter(CIRCUIT_BREAKER_CALLS_TOTAL, Arrays.asList(methodTag, CIRCUIT_BREAKER_RESULT_CB_OPEN));

                registry.counter(CIRCUIT_BREAKER_OPENED_TOTAL, methodTagSingleton);
            }

            if (operation.hasBulkhead()) {
                registry.counter(BULKHEAD_CALLS_TOTAL, Arrays.asList(methodTag, BULKHEAD_RESULT_ACCEPTED));
                registry.counter(BULKHEAD_CALLS_TOTAL, Arrays.asList(methodTag, BULKHEAD_RESULT_REJECTED));

                registry.timer(BULKHEAD_RUNNING_DURATION, methodTagSingleton);
                if (specCompatibility.isOperationTrulyOrPseudoAsynchronous(operation)) {
                    registry.timer(BULKHEAD_WAITING_DURATION, methodTagSingleton);
                }
            }

            if (operation.hasRateLimit()) {
                registry.counter(RATE_LIMIT_CALLS_TOTAL, Arrays.asList(methodTag, BULKHEAD_RESULT_ACCEPTED));
                registry.counter(RATE_LIMIT_CALLS_TOTAL, Arrays.asList(methodTag, BULKHEAD_RESULT_REJECTED));
            }
        }

        private void registerGauge(LongSupplier supplier, String name, Tag... tags) {
            registry.gauge(name, Arrays.asList(tags), supplier, it -> (double) it.getAsLong());
        }

        private void registerGauge(BooleanSupplier supplier, String name, Tag... tags) {
            registry.gauge(name, Arrays.asList(tags), supplier, it -> it.getAsBoolean() ? 1.0 : 0.0);
        }

        private void registerTimeGauge(LongSupplier supplier, String name, Tag... tags) {
            registry.more().timeGauge(name, Arrays.asList(tags), supplier, TimeUnit.NANOSECONDS,
                    it -> (double) it.getAsLong());
        }

        // ---

        @Override
        public void executionFinished(boolean succeeded, boolean fallbackDefined, boolean fallbackApplied) {
            Tag resultTag = succeeded ? RESULT_VALUE_RETURNED : RESULT_EXCEPTION_THROWN;
            Tag fallbackTag = fallbackDefined
                    ? (fallbackApplied ? FALLBACK_APPLIED : FALLBACK_NOT_APPLIED)
                    : FALLBACK_NOT_DEFINED;
            registry.counter(INVOCATIONS_TOTAL, Arrays.asList(methodTag, resultTag, fallbackTag)).increment();
        }

        @Override
        public void retryAttempted() {
            registry.counter(RETRY_RETRIES_TOTAL, methodTagSingleton).increment();
        }

        @Override
        public void retryValueReturned(boolean retried) {
            registry.counter(RETRY_CALLS_TOTAL, Arrays.asList(methodTag, retried ? RETRIED_TRUE : RETRIED_FALSE,
                    RETRY_RESULT_VALUE_RETURNED)).increment();
        }

        @Override
        public void retryExceptionNotRetryable(boolean retried) {
            registry.counter(RETRY_CALLS_TOTAL, Arrays.asList(methodTag, retried ? RETRIED_TRUE : RETRIED_FALSE,
                    RETRY_RESULT_EXCEPTION_NOT_RETRYABLE)).increment();
        }

        @Override
        public void retryMaxRetriesReached(boolean retried) {
            registry.counter(RETRY_CALLS_TOTAL, Arrays.asList(methodTag, retried ? RETRIED_TRUE : RETRIED_FALSE,
                    RETRY_RESULT_MAX_RETRIES_REACHED)).increment();
        }

        @Override
        public void retryMaxDurationReached(boolean retried) {
            registry.counter(RETRY_CALLS_TOTAL, Arrays.asList(methodTag, retried ? RETRIED_TRUE : RETRIED_FALSE,
                    RETRY_RESULT_MAX_DURATION_REACHED)).increment();
        }

        @Override
        public void timeoutFinished(boolean timedOut, long time) {
            registry.counter(TIMEOUT_CALLS_TOTAL, Arrays.asList(methodTag, timedOut ? TIMED_OUT_TRUE : TIMED_OUT_FALSE))
                    .increment();
            registry.timer(TIMEOUT_EXECUTION_DURATION, methodTagSingleton).record(time, TimeUnit.NANOSECONDS);
        }

        @Override
        public void circuitBreakerFinished(CircuitBreakerEvents.Result result) {
            Tag circuitBreakerResultTag = null;
            switch (result) {
                case SUCCESS:
                    circuitBreakerResultTag = CIRCUIT_BREAKER_RESULT_SUCCESS;
                    break;
                case FAILURE:
                    circuitBreakerResultTag = CIRCUIT_BREAKER_RESULT_FAILURE;
                    break;
                case PREVENTED:
                    circuitBreakerResultTag = CIRCUIT_BREAKER_RESULT_CB_OPEN;
                    break;
            }
            registry.counter(CIRCUIT_BREAKER_CALLS_TOTAL, Arrays.asList(methodTag, circuitBreakerResultTag)).increment();
        }

        @Override
        public void circuitBreakerMovedToOpen() {
            registry.counter(CIRCUIT_BREAKER_OPENED_TOTAL, methodTagSingleton).increment();
        }

        @Override
        public void registerCircuitBreakerIsClosed(BooleanSupplier supplier) {
            registerGauge(supplier, CIRCUIT_BREAKER_STATE_CURRENT, methodTag, CIRCUIT_BREAKER_STATE_CLOSED);
        }

        @Override
        public void registerCircuitBreakerIsOpen(BooleanSupplier supplier) {
            registerGauge(supplier, CIRCUIT_BREAKER_STATE_CURRENT, methodTag, CIRCUIT_BREAKER_STATE_OPEN);
        }

        @Override
        public void registerCircuitBreakerIsHalfOpen(BooleanSupplier supplier) {
            registerGauge(supplier, CIRCUIT_BREAKER_STATE_CURRENT, methodTag, CIRCUIT_BREAKER_STATE_HALF_OPEN);
        }

        @Override
        public void registerCircuitBreakerTimeSpentInClosed(LongSupplier supplier) {
            registerTimeGauge(supplier, CIRCUIT_BREAKER_STATE_TOTAL,
                    methodTag, CIRCUIT_BREAKER_STATE_CLOSED);
        }

        @Override
        public void registerCircuitBreakerTimeSpentInOpen(LongSupplier supplier) {
            registerTimeGauge(supplier, CIRCUIT_BREAKER_STATE_TOTAL,
                    methodTag, CIRCUIT_BREAKER_STATE_OPEN);
        }

        @Override
        public void registerCircuitBreakerTimeSpentInHalfOpen(LongSupplier supplier) {
            registerTimeGauge(supplier, CIRCUIT_BREAKER_STATE_TOTAL,
                    methodTag, CIRCUIT_BREAKER_STATE_HALF_OPEN);
        }

        @Override
        public void bulkheadDecisionMade(boolean accepted) {
            Tag bulkheadResultTag = accepted ? BULKHEAD_RESULT_ACCEPTED : BULKHEAD_RESULT_REJECTED;
            registry.counter(BULKHEAD_CALLS_TOTAL, Arrays.asList(methodTag, bulkheadResultTag)).increment();
        }

        @Override
        public void registerBulkheadExecutionsRunning(LongSupplier supplier) {
            registerGauge(supplier, BULKHEAD_EXECUTIONS_RUNNING, methodTag);
        }

        @Override
        public void registerBulkheadExecutionsWaiting(LongSupplier supplier) {
            registerGauge(supplier, BULKHEAD_EXECUTIONS_WAITING, methodTag);
        }

        @Override
        public void updateBulkheadRunningDuration(long time) {
            registry.timer(BULKHEAD_RUNNING_DURATION, methodTagSingleton).record(time, TimeUnit.NANOSECONDS);
        }

        @Override
        public void updateBulkheadWaitingDuration(long time) {
            registry.timer(BULKHEAD_WAITING_DURATION, methodTagSingleton).record(time, TimeUnit.NANOSECONDS);
        }

        @Override
        public void rateLimitDecisionMade(boolean permitted) {
            Tag rateLimitResultTag = permitted ? RATE_LIMIT_RESULT_PERMITTED : RATE_LIMIT_RESULT_REJECTED;
            registry.counter(RATE_LIMIT_CALLS_TOTAL, Arrays.asList(methodTag, rateLimitResultTag)).increment();
        }
    }
}
