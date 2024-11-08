package io.smallrye.faulttolerance.core.metrics;

import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.BULKHEAD_CALLS_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.BULKHEAD_EXECUTIONS_RUNNING;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.BULKHEAD_EXECUTIONS_WAITING;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.BULKHEAD_RUNNING_DURATION;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.BULKHEAD_WAITING_DURATION;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.CIRCUIT_BREAKER_CALLS_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.CIRCUIT_BREAKER_OPENED_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.CIRCUIT_BREAKER_STATE_CURRENT;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.CIRCUIT_BREAKER_STATE_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.INVOCATIONS_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.RATE_LIMIT_CALLS_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.RETRY_CALLS_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.RETRY_RETRIES_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.TIMEOUT_CALLS_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.TIMEOUT_EXECUTION_DURATION;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerEvents;

public class MicrometerRecorder implements MetricsRecorder {
    private static final Tag RESULT_VALUE_RETURNED = Tag.of("result", "valueReturned");
    private static final Tag RESULT_EXCEPTION_THROWN = Tag.of("result", "exceptionThrown");

    private static final Tag FALLBACK_APPLIED = Tag.of("fallback", "applied");
    private static final Tag FALLBACK_NOT_APPLIED = Tag.of("fallback", "notApplied");
    private static final Tag FALLBACK_NOT_DEFINED = Tag.of("fallback", "notDefined");

    private static final Tag RETRIED_TRUE = Tag.of("retried", "true");
    private static final Tag RETRIED_FALSE = Tag.of("retried", "false");
    private static final Tag RETRY_RESULT_VALUE_RETURNED = Tag.of("retryResult", "valueReturned");
    private static final Tag RETRY_RESULT_EXCEPTION_NOT_RETRYABLE = Tag.of("retryResult", "exceptionNotRetryable");
    private static final Tag RETRY_RESULT_MAX_RETRIES_REACHED = Tag.of("retryResult", "maxRetriesReached");
    private static final Tag RETRY_RESULT_MAX_DURATION_REACHED = Tag.of("retryResult", "maxDurationReached");

    private static final Tag TIMED_OUT_TRUE = Tag.of("timedOut", "true");
    private static final Tag TIMED_OUT_FALSE = Tag.of("timedOut", "false");

    private static final Tag CIRCUIT_BREAKER_RESULT_SUCCESS = Tag.of("circuitBreakerResult", "success");
    private static final Tag CIRCUIT_BREAKER_RESULT_FAILURE = Tag.of("circuitBreakerResult", "failure");
    private static final Tag CIRCUIT_BREAKER_RESULT_CB_OPEN = Tag.of("circuitBreakerResult", "circuitBreakerOpen");

    private static final Tag CIRCUIT_BREAKER_STATE_CLOSED = Tag.of("state", "closed");
    private static final Tag CIRCUIT_BREAKER_STATE_OPEN = Tag.of("state", "open");
    private static final Tag CIRCUIT_BREAKER_STATE_HALF_OPEN = Tag.of("state", "halfOpen");

    private static final Tag BULKHEAD_RESULT_ACCEPTED = Tag.of("bulkheadResult", "accepted");
    private static final Tag BULKHEAD_RESULT_REJECTED = Tag.of("bulkheadResult", "rejected");

    private static final Tag RATE_LIMIT_RESULT_PERMITTED = Tag.of("rateLimitResult", "permitted");
    private static final Tag RATE_LIMIT_RESULT_REJECTED = Tag.of("rateLimitResult", "rejected");

    private final MeterRegistry registry;
    private final Tag methodTag;
    private final Iterable<Tag> methodTagSingleton;

    public MicrometerRecorder(MeterRegistry registry, MeteredOperation operation) {
        this.registry = registry;
        this.methodTag = Tag.of("method", operation.name());
        this.methodTagSingleton = Collections.singleton(methodTag);

        registerMetrics(operation);
    }

    private void registerMetrics(MeteredOperation operation) {
        // make sure all applicable metrics for given method are registered eagerly
        // we only touch counters and histograms, because gauges are registered eagerly elsewhere

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
            registry.counter(RETRY_CALLS_TOTAL, Arrays.asList(methodTag, RETRIED_FALSE, RETRY_RESULT_EXCEPTION_NOT_RETRYABLE));
            registry.counter(RETRY_CALLS_TOTAL, Arrays.asList(methodTag, RETRIED_FALSE, RETRY_RESULT_MAX_RETRIES_REACHED));
            registry.counter(RETRY_CALLS_TOTAL, Arrays.asList(methodTag, RETRIED_FALSE, RETRY_RESULT_MAX_DURATION_REACHED));
            registry.counter(RETRY_CALLS_TOTAL, Arrays.asList(methodTag, RETRIED_TRUE, RETRY_RESULT_VALUE_RETURNED));
            registry.counter(RETRY_CALLS_TOTAL, Arrays.asList(methodTag, RETRIED_TRUE, RETRY_RESULT_EXCEPTION_NOT_RETRYABLE));
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
            if (operation.mayBeAsynchronous()) {
                registry.timer(BULKHEAD_WAITING_DURATION, methodTagSingleton);
            }
        }

        if (operation.hasRateLimit()) {
            registry.counter(RATE_LIMIT_CALLS_TOTAL, Arrays.asList(methodTag, BULKHEAD_RESULT_ACCEPTED));
            registry.counter(RATE_LIMIT_CALLS_TOTAL, Arrays.asList(methodTag, BULKHEAD_RESULT_REJECTED));
        }
    }

    // Micrometer only refers to the state object (our supplier) behind the gauge weakly,
    // so we refer to it strongly from the value extraction function
    //
    // this means that the state object never becomes unreachable, which in turn means
    // that the gauge always has a value
    //
    // Micrometer has an API to make sure the state object is held strongly, but that
    // has more overhead (and the API that needs to be used is uglier)

    private void registerGauge(LongSupplier supplier, String name, Tag... tags) {
        registry.gauge(name, Arrays.asList(tags), supplier, ignored -> (double) supplier.getAsLong());
    }

    private void registerGauge(BooleanSupplier supplier, String name, Tag... tags) {
        registry.gauge(name, Arrays.asList(tags), supplier, ignored -> supplier.getAsBoolean() ? 1.0 : 0.0);
    }

    private void registerTimeGauge(LongSupplier supplier, String name, Tag... tags) {
        registry.more().timeGauge(name, Arrays.asList(tags), supplier, TimeUnit.NANOSECONDS,
                ignored -> (double) supplier.getAsLong());
    }

    // ---

    @Override
    public void executionFinished(boolean succeeded, boolean fallbackDefined, boolean fallbackApplied) {
        Tag resultTag = succeeded ? RESULT_VALUE_RETURNED : RESULT_EXCEPTION_THROWN;
        Tag fallbackTag = fallbackDefined ? (fallbackApplied ? FALLBACK_APPLIED : FALLBACK_NOT_APPLIED) : FALLBACK_NOT_DEFINED;
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
        registerTimeGauge(supplier, CIRCUIT_BREAKER_STATE_TOTAL, methodTag, CIRCUIT_BREAKER_STATE_CLOSED);
    }

    @Override
    public void registerCircuitBreakerTimeSpentInOpen(LongSupplier supplier) {
        registerTimeGauge(supplier, CIRCUIT_BREAKER_STATE_TOTAL, methodTag, CIRCUIT_BREAKER_STATE_OPEN);
    }

    @Override
    public void registerCircuitBreakerTimeSpentInHalfOpen(LongSupplier supplier) {
        registerTimeGauge(supplier, CIRCUIT_BREAKER_STATE_TOTAL, methodTag, CIRCUIT_BREAKER_STATE_HALF_OPEN);
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
