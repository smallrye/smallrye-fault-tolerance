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

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerEvents;

public class OpenTelemetryRecorder implements MetricsRecorder {
    private static final List<Double> BUCKET_BOUNDARIES = List.of(
            0.005, 0.01,
            0.025, 0.05, 0.075, 0.1,
            0.25, 0.5, 0.75, 1.0,
            2.5, 5.0, 7.5, 10.0);
    private static final double NANOS_TO_SECONDS = 1.0 / 1_000_000_000.0;

    private static final AttributeKey<String> METHOD = AttributeKey.stringKey("method");

    private static final AttributeKey<String> RESULT = AttributeKey.stringKey("result");
    private static final String RESULT_VALUE_RETURNED = "valueReturned";
    private static final String RESULT_EXCEPTION_THROWN = "exceptionThrown";

    private static final AttributeKey<String> FALLBACK = AttributeKey.stringKey("fallback");
    private static final String FALLBACK_APPLIED = "applied";
    private static final String FALLBACK_NOT_APPLIED = "notApplied";
    private static final String FALLBACK_NOT_DEFINED = "notDefined";

    private static final AttributeKey<String> RETRIED = AttributeKey.stringKey("retried");
    private static final String RETRIED_TRUE = "true";
    private static final String RETRIED_FALSE = "false";
    private static final AttributeKey<String> RETRY_RESULT = AttributeKey.stringKey("retryResult");
    private static final String RETRY_RESULT_VALUE_RETURNED = "valueReturned";
    private static final String RETRY_RESULT_EXCEPTION_NOT_RETRYABLE = "exceptionNotRetryable";
    private static final String RETRY_RESULT_MAX_RETRIES_REACHED = "maxRetriesReached";
    private static final String RETRY_RESULT_MAX_DURATION_REACHED = "maxDurationReached";

    private static final AttributeKey<String> TIMED_OUT = AttributeKey.stringKey("timedOut");
    private static final String TIMED_OUT_TRUE = "true";
    private static final String TIMED_OUT_FALSE = "false";

    private static final AttributeKey<String> CIRCUIT_BREAKER_RESULT = AttributeKey.stringKey("circuitBreakerResult");
    private static final String CIRCUIT_BREAKER_RESULT_SUCCESS = "success";
    private static final String CIRCUIT_BREAKER_RESULT_FAILURE = "failure";
    private static final String CIRCUIT_BREAKER_RESULT_CB_OPEN = "circuitBreakerOpen";

    private static final AttributeKey<String> CIRCUIT_BREAKER_STATE = AttributeKey.stringKey("state");
    private static final String CIRCUIT_BREAKER_STATE_CLOSED = "closed";
    private static final String CIRCUIT_BREAKER_STATE_OPEN = "open";
    private static final String CIRCUIT_BREAKER_STATE_HALF_OPEN = "halfOpen";

    private static final AttributeKey<String> BULKHEAD_RESULT = AttributeKey.stringKey("bulkheadResult");
    private static final String BULKHEAD_RESULT_ACCEPTED = "accepted";
    private static final String BULKHEAD_RESULT_REJECTED = "rejected";

    private static final AttributeKey<String> RATE_LIMIT_RESULT = AttributeKey.stringKey("rateLimitResult");
    private static final String RATE_LIMIT_RESULT_PERMITTED = "permitted";
    private static final String RATE_LIMIT_RESULT_REJECTED = "rejected";

    private final Meter meter;
    private final String methodName;

    private final LongCounter invocationsTotal;
    private final LongCounter retryCallsTotal;
    private final LongCounter retryRetriesTotal;
    private final LongCounter timeoutCallsTotal;
    private final DoubleHistogram timeoutExecutionDuration;
    private final LongCounter circuitBreakerCallsTotal;
    private final LongCounter circuitBreakerOpenedTotal;
    private final LongCounter bulkheadCallsTotal;
    private final DoubleHistogram bulkheadRunningDuration;
    private final DoubleHistogram bulkheadWaitingDuration;
    private final LongCounter rateLimitCallsTotal;

    public OpenTelemetryRecorder(Meter meter, MeteredOperation operation) {
        this.meter = meter;
        this.methodName = operation.name();

        // make sure all applicable metrics for given method are registered eagerly
        // we only touch sync metrics, because async metrics are registered eagerly elsewhere

        this.invocationsTotal = meter.counterBuilder(INVOCATIONS_TOTAL).build();

        if (operation.hasRetry()) {
            this.retryCallsTotal = meter.counterBuilder(RETRY_CALLS_TOTAL).build();
            this.retryRetriesTotal = meter.counterBuilder(RETRY_RETRIES_TOTAL).build();
        } else {
            this.retryCallsTotal = null;
            this.retryRetriesTotal = null;
        }

        if (operation.hasTimeout()) {
            this.timeoutCallsTotal = meter.counterBuilder(TIMEOUT_CALLS_TOTAL).build();
            this.timeoutExecutionDuration = meter.histogramBuilder(TIMEOUT_EXECUTION_DURATION)
                    .setUnit("seconds")
                    .setExplicitBucketBoundariesAdvice(BUCKET_BOUNDARIES)
                    .build();
        } else {
            this.timeoutCallsTotal = null;
            this.timeoutExecutionDuration = null;
        }

        if (operation.hasCircuitBreaker()) {
            this.circuitBreakerCallsTotal = meter.counterBuilder(CIRCUIT_BREAKER_CALLS_TOTAL).build();
            this.circuitBreakerOpenedTotal = meter.counterBuilder(CIRCUIT_BREAKER_OPENED_TOTAL).build();
        } else {
            this.circuitBreakerCallsTotal = null;
            this.circuitBreakerOpenedTotal = null;
        }

        if (operation.hasBulkhead()) {
            this.bulkheadCallsTotal = meter.counterBuilder(BULKHEAD_CALLS_TOTAL).build();
            this.bulkheadRunningDuration = meter.histogramBuilder(BULKHEAD_RUNNING_DURATION)
                    .setUnit("seconds")
                    .setExplicitBucketBoundariesAdvice(BUCKET_BOUNDARIES)
                    .build();
            if (operation.isAsynchronous()) {
                this.bulkheadWaitingDuration = meter.histogramBuilder(BULKHEAD_WAITING_DURATION)
                        .setUnit("seconds")
                        .setExplicitBucketBoundariesAdvice(BUCKET_BOUNDARIES)
                        .build();
            } else {
                this.bulkheadWaitingDuration = null;
            }
        } else {
            this.bulkheadCallsTotal = null;
            this.bulkheadRunningDuration = null;
            this.bulkheadWaitingDuration = null;
        }

        if (operation.hasRateLimit()) {
            this.rateLimitCallsTotal = meter.counterBuilder(RATE_LIMIT_CALLS_TOTAL).build();
        } else {
            this.rateLimitCallsTotal = null;
        }
    }

    private void registerAsyncUpDownCounter(LongSupplier supplier, String name, Attributes attributes) {
        meter.upDownCounterBuilder(name).buildWithCallback(m -> m.record(supplier.getAsLong(), attributes));
    }

    private void registerAsyncUpDownCounter(BooleanSupplier supplier, String name, Attributes attributes) {
        meter.upDownCounterBuilder(name).buildWithCallback(m -> m.record(supplier.getAsBoolean() ? 1 : 0, attributes));
    }

    private void registerAsyncCounter(LongSupplier supplier, String name, String unit, Attributes attributes) {
        meter.counterBuilder(name).setUnit(unit).buildWithCallback(m -> m.record(supplier.getAsLong(), attributes));
    }

    // ---

    @Override
    public void executionFinished(boolean succeeded, boolean fallbackDefined, boolean fallbackApplied) {
        String fallback = fallbackDefined
                ? (fallbackApplied ? FALLBACK_APPLIED : FALLBACK_NOT_APPLIED)
                : FALLBACK_NOT_DEFINED;

        invocationsTotal.add(1, Attributes.of(
                METHOD, methodName,
                RESULT, succeeded ? RESULT_VALUE_RETURNED : RESULT_EXCEPTION_THROWN,
                FALLBACK, fallback));
    }

    @Override
    public void retryAttempted() {
        retryRetriesTotal.add(1, Attributes.of(METHOD, methodName));
    }

    @Override
    public void retryValueReturned(boolean retried) {
        retryCallsTotal.add(1, Attributes.of(
                METHOD, methodName,
                RETRIED, retried ? RETRIED_TRUE : RETRIED_FALSE,
                RETRY_RESULT, RETRY_RESULT_VALUE_RETURNED));
    }

    @Override
    public void retryExceptionNotRetryable(boolean retried) {
        retryCallsTotal.add(1, Attributes.of(
                METHOD, methodName,
                RETRIED, retried ? RETRIED_TRUE : RETRIED_FALSE,
                RETRY_RESULT, RETRY_RESULT_EXCEPTION_NOT_RETRYABLE));
    }

    @Override
    public void retryMaxRetriesReached(boolean retried) {
        retryCallsTotal.add(1, Attributes.of(
                METHOD, methodName,
                RETRIED, retried ? RETRIED_TRUE : RETRIED_FALSE,
                RETRY_RESULT, RETRY_RESULT_MAX_RETRIES_REACHED));
    }

    @Override
    public void retryMaxDurationReached(boolean retried) {
        retryCallsTotal.add(1, Attributes.of(
                METHOD, methodName,
                RETRIED, retried ? RETRIED_TRUE : RETRIED_FALSE,
                RETRY_RESULT, RETRY_RESULT_MAX_DURATION_REACHED));
    }

    @Override
    public void timeoutFinished(boolean timedOut, long time) {
        timeoutCallsTotal.add(1, Attributes.of(
                METHOD, methodName,
                TIMED_OUT, timedOut ? TIMED_OUT_TRUE : TIMED_OUT_FALSE));
        timeoutExecutionDuration.record(time * NANOS_TO_SECONDS, Attributes.of(METHOD, methodName));
    }

    @Override
    public void circuitBreakerFinished(CircuitBreakerEvents.Result result) {
        String circuitBreakerResult = null;
        switch (result) {
            case SUCCESS:
                circuitBreakerResult = CIRCUIT_BREAKER_RESULT_SUCCESS;
                break;
            case FAILURE:
                circuitBreakerResult = CIRCUIT_BREAKER_RESULT_FAILURE;
                break;
            case PREVENTED:
                circuitBreakerResult = CIRCUIT_BREAKER_RESULT_CB_OPEN;
                break;
        }
        circuitBreakerCallsTotal.add(1, Attributes.of(
                METHOD, methodName,
                CIRCUIT_BREAKER_RESULT, circuitBreakerResult));
    }

    @Override
    public void circuitBreakerMovedToOpen() {
        circuitBreakerOpenedTotal.add(1, Attributes.of(METHOD, methodName));
    }

    @Override
    public void registerCircuitBreakerIsClosed(BooleanSupplier supplier) {
        registerAsyncUpDownCounter(supplier, CIRCUIT_BREAKER_STATE_CURRENT, Attributes.of(
                METHOD, methodName,
                CIRCUIT_BREAKER_STATE, CIRCUIT_BREAKER_STATE_CLOSED));
    }

    @Override
    public void registerCircuitBreakerIsOpen(BooleanSupplier supplier) {
        registerAsyncUpDownCounter(supplier, CIRCUIT_BREAKER_STATE_CURRENT, Attributes.of(
                METHOD, methodName,
                CIRCUIT_BREAKER_STATE, CIRCUIT_BREAKER_STATE_OPEN));
    }

    @Override
    public void registerCircuitBreakerIsHalfOpen(BooleanSupplier supplier) {
        registerAsyncUpDownCounter(supplier, CIRCUIT_BREAKER_STATE_CURRENT, Attributes.of(
                METHOD, methodName,
                CIRCUIT_BREAKER_STATE, CIRCUIT_BREAKER_STATE_HALF_OPEN));
    }

    @Override
    public void registerCircuitBreakerTimeSpentInClosed(LongSupplier supplier) {
        registerAsyncCounter(supplier, CIRCUIT_BREAKER_STATE_TOTAL, "nanoseconds", Attributes.of(
                METHOD, methodName,
                CIRCUIT_BREAKER_STATE, CIRCUIT_BREAKER_STATE_CLOSED));
    }

    @Override
    public void registerCircuitBreakerTimeSpentInOpen(LongSupplier supplier) {
        registerAsyncCounter(supplier, CIRCUIT_BREAKER_STATE_TOTAL, "nanoseconds", Attributes.of(
                METHOD, methodName,
                CIRCUIT_BREAKER_STATE, CIRCUIT_BREAKER_STATE_OPEN));
    }

    @Override
    public void registerCircuitBreakerTimeSpentInHalfOpen(LongSupplier supplier) {
        registerAsyncCounter(supplier, CIRCUIT_BREAKER_STATE_TOTAL, "nanoseconds", Attributes.of(
                METHOD, methodName,
                CIRCUIT_BREAKER_STATE, CIRCUIT_BREAKER_STATE_HALF_OPEN));
    }

    @Override
    public void bulkheadDecisionMade(boolean accepted) {
        bulkheadCallsTotal.add(1, Attributes.of(
                METHOD, methodName,
                BULKHEAD_RESULT, accepted ? BULKHEAD_RESULT_ACCEPTED : BULKHEAD_RESULT_REJECTED));
    }

    @Override
    public void registerBulkheadExecutionsRunning(LongSupplier supplier) {
        registerAsyncUpDownCounter(supplier, BULKHEAD_EXECUTIONS_RUNNING, Attributes.of(METHOD, methodName));
    }

    @Override
    public void registerBulkheadExecutionsWaiting(LongSupplier supplier) {
        registerAsyncUpDownCounter(supplier, BULKHEAD_EXECUTIONS_WAITING, Attributes.of(METHOD, methodName));
    }

    @Override
    public void updateBulkheadRunningDuration(long time) {
        bulkheadRunningDuration.record(time * NANOS_TO_SECONDS, Attributes.of(METHOD, methodName));
    }

    @Override
    public void updateBulkheadWaitingDuration(long time) {
        bulkheadWaitingDuration.record(time * NANOS_TO_SECONDS, Attributes.of(METHOD, methodName));
    }

    @Override
    public void rateLimitDecisionMade(boolean permitted) {
        rateLimitCallsTotal.add(1, Attributes.of(
                METHOD, methodName,
                RATE_LIMIT_RESULT, permitted ? RATE_LIMIT_RESULT_PERMITTED : RATE_LIMIT_RESULT_REJECTED));
    }
}
