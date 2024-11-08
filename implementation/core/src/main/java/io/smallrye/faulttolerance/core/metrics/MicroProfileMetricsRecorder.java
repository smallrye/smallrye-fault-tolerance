package io.smallrye.faulttolerance.core.metrics;

import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.BULKHEAD_CALLS_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.BULKHEAD_EXECUTIONS_RUNNING;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.BULKHEAD_EXECUTIONS_WAITING;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.CIRCUIT_BREAKER_CALLS_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.CIRCUIT_BREAKER_OPENED_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.CIRCUIT_BREAKER_STATE_CURRENT;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.CIRCUIT_BREAKER_STATE_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.INVOCATIONS_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.RATE_LIMIT_CALLS_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.RETRY_CALLS_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.RETRY_RETRIES_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.TIMEOUT_CALLS_TOTAL;

import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;

import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerEvents;

public class MicroProfileMetricsRecorder implements MetricsRecorder {
    private static final Metadata TIMEOUT_EXECUTION_DURATION_METADATA = Metadata.builder()
            .withName(MetricsConstants.TIMEOUT_EXECUTION_DURATION)
            .withUnit(MetricUnits.NANOSECONDS)
            .build();

    private static final Metadata BULKHEAD_RUNNING_DURATION_METADATA = Metadata.builder()
            .withName(MetricsConstants.BULKHEAD_RUNNING_DURATION)
            .withUnit(MetricUnits.NANOSECONDS)
            .build();

    private static final Metadata BULKHEAD_WAITING_DURATION_METADATA = Metadata.builder()
            .withName(MetricsConstants.BULKHEAD_WAITING_DURATION)
            .withUnit(MetricUnits.NANOSECONDS)
            .build();

    private static final Tag RESULT_VALUE_RETURNED = new Tag("result", "valueReturned");
    private static final Tag RESULT_EXCEPTION_THROWN = new Tag("result", "exceptionThrown");

    private static final Tag FALLBACK_APPLIED = new Tag("fallback", "applied");
    private static final Tag FALLBACK_NOT_APPLIED = new Tag("fallback", "notApplied");
    private static final Tag FALLBACK_NOT_DEFINED = new Tag("fallback", "notDefined");

    private static final Tag RETRIED_TRUE = new Tag("retried", "true");
    private static final Tag RETRIED_FALSE = new Tag("retried", "false");
    private static final Tag RETRY_RESULT_VALUE_RETURNED = new Tag("retryResult", "valueReturned");
    private static final Tag RETRY_RESULT_EXCEPTION_NOT_RETRYABLE = new Tag("retryResult", "exceptionNotRetryable");
    private static final Tag RETRY_RESULT_MAX_RETRIES_REACHED = new Tag("retryResult", "maxRetriesReached");
    private static final Tag RETRY_RESULT_MAX_DURATION_REACHED = new Tag("retryResult", "maxDurationReached");

    private static final Tag TIMED_OUT_TRUE = new Tag("timedOut", "true");
    private static final Tag TIMED_OUT_FALSE = new Tag("timedOut", "false");

    private static final Tag CIRCUIT_BREAKER_RESULT_SUCCESS = new Tag("circuitBreakerResult", "success");
    private static final Tag CIRCUIT_BREAKER_RESULT_FAILURE = new Tag("circuitBreakerResult", "failure");
    private static final Tag CIRCUIT_BREAKER_RESULT_CB_OPEN = new Tag("circuitBreakerResult", "circuitBreakerOpen");

    private static final Tag CIRCUIT_BREAKER_STATE_CLOSED = new Tag("state", "closed");
    private static final Tag CIRCUIT_BREAKER_STATE_OPEN = new Tag("state", "open");
    private static final Tag CIRCUIT_BREAKER_STATE_HALF_OPEN = new Tag("state", "halfOpen");

    private static final Tag BULKHEAD_RESULT_ACCEPTED = new Tag("bulkheadResult", "accepted");
    private static final Tag BULKHEAD_RESULT_REJECTED = new Tag("bulkheadResult", "rejected");

    private static final Tag RATE_LIMIT_RESULT_PERMITTED = new Tag("rateLimitResult", "permitted");
    private static final Tag RATE_LIMIT_RESULT_REJECTED = new Tag("rateLimitResult", "rejected");

    private final MetricRegistry registry;
    private final Tag methodTag;

    public MicroProfileMetricsRecorder(MetricRegistry registry, MeteredOperation operation) {
        this.registry = registry;
        this.methodTag = new Tag("method", operation.name());

        registerMetrics(operation);
    }

    private void registerMetrics(MeteredOperation operation) {
        // make sure all applicable metrics for given method are registered eagerly
        // we only touch counters and histograms, because gauges are registered eagerly otherwise

        if (operation.hasFallback()) {
            registry.counter(INVOCATIONS_TOTAL, methodTag, RESULT_VALUE_RETURNED, FALLBACK_NOT_APPLIED).getCount();
            registry.counter(INVOCATIONS_TOTAL, methodTag, RESULT_VALUE_RETURNED, FALLBACK_APPLIED).getCount();
            registry.counter(INVOCATIONS_TOTAL, methodTag, RESULT_EXCEPTION_THROWN, FALLBACK_NOT_APPLIED).getCount();
            registry.counter(INVOCATIONS_TOTAL, methodTag, RESULT_EXCEPTION_THROWN, FALLBACK_APPLIED).getCount();
        } else {
            registry.counter(INVOCATIONS_TOTAL, methodTag, RESULT_VALUE_RETURNED, FALLBACK_NOT_DEFINED).getCount();
            registry.counter(INVOCATIONS_TOTAL, methodTag, RESULT_EXCEPTION_THROWN, FALLBACK_NOT_DEFINED).getCount();
        }

        if (operation.hasRetry()) {
            registry.counter(RETRY_RETRIES_TOTAL, methodTag).getCount();

            registry.counter(RETRY_CALLS_TOTAL, methodTag, RETRIED_FALSE, RETRY_RESULT_VALUE_RETURNED).getCount();
            registry.counter(RETRY_CALLS_TOTAL, methodTag, RETRIED_FALSE, RETRY_RESULT_EXCEPTION_NOT_RETRYABLE).getCount();
            registry.counter(RETRY_CALLS_TOTAL, methodTag, RETRIED_FALSE, RETRY_RESULT_MAX_RETRIES_REACHED).getCount();
            registry.counter(RETRY_CALLS_TOTAL, methodTag, RETRIED_FALSE, RETRY_RESULT_MAX_DURATION_REACHED).getCount();
            registry.counter(RETRY_CALLS_TOTAL, methodTag, RETRIED_TRUE, RETRY_RESULT_VALUE_RETURNED).getCount();
            registry.counter(RETRY_CALLS_TOTAL, methodTag, RETRIED_TRUE, RETRY_RESULT_EXCEPTION_NOT_RETRYABLE).getCount();
            registry.counter(RETRY_CALLS_TOTAL, methodTag, RETRIED_TRUE, RETRY_RESULT_MAX_RETRIES_REACHED).getCount();
            registry.counter(RETRY_CALLS_TOTAL, methodTag, RETRIED_TRUE, RETRY_RESULT_MAX_DURATION_REACHED).getCount();
        }

        if (operation.hasTimeout()) {
            registry.counter(TIMEOUT_CALLS_TOTAL, methodTag, TIMED_OUT_TRUE).getCount();
            registry.counter(TIMEOUT_CALLS_TOTAL, methodTag, TIMED_OUT_FALSE).getCount();

            registry.histogram(TIMEOUT_EXECUTION_DURATION_METADATA, methodTag).getCount();
        }

        if (operation.hasCircuitBreaker()) {
            registry.counter(CIRCUIT_BREAKER_CALLS_TOTAL, methodTag, CIRCUIT_BREAKER_RESULT_SUCCESS).getCount();
            registry.counter(CIRCUIT_BREAKER_CALLS_TOTAL, methodTag, CIRCUIT_BREAKER_RESULT_FAILURE).getCount();
            registry.counter(CIRCUIT_BREAKER_CALLS_TOTAL, methodTag, CIRCUIT_BREAKER_RESULT_CB_OPEN).getCount();

            registry.counter(CIRCUIT_BREAKER_OPENED_TOTAL, methodTag).getCount();
        }

        if (operation.hasBulkhead()) {
            registry.counter(BULKHEAD_CALLS_TOTAL, methodTag, BULKHEAD_RESULT_ACCEPTED).getCount();
            registry.counter(BULKHEAD_CALLS_TOTAL, methodTag, BULKHEAD_RESULT_REJECTED).getCount();

            registry.histogram(BULKHEAD_RUNNING_DURATION_METADATA, methodTag).getCount();
            if (operation.mayBeAsynchronous()) {
                registry.histogram(BULKHEAD_WAITING_DURATION_METADATA, methodTag).getCount();
            }
        }

        if (operation.hasRateLimit()) {
            registry.counter(RATE_LIMIT_CALLS_TOTAL, methodTag, RATE_LIMIT_RESULT_PERMITTED).getCount();
            registry.counter(RATE_LIMIT_CALLS_TOTAL, methodTag, RATE_LIMIT_RESULT_REJECTED).getCount();
        }
    }

    private void registerGauge(BooleanSupplier supplier, String name, String unit, Tag... tags) {
        registerGauge(() -> supplier.getAsBoolean() ? 1L : 0L, name, unit, tags);
    }

    private void registerGauge(LongSupplier supplier, String name, String unit, Tag... tags) {
        Metadata metadata = Metadata.builder()
                .withName(name)
                .withUnit(unit)
                .build();
        registry.gauge(metadata, supplier::getAsLong, tags);
    }

    // ---

    @Override
    public void executionFinished(boolean succeeded, boolean fallbackDefined, boolean fallbackApplied) {
        Tag resultTag = succeeded ? RESULT_VALUE_RETURNED : RESULT_EXCEPTION_THROWN;
        Tag fallbackTag = fallbackDefined ? (fallbackApplied ? FALLBACK_APPLIED : FALLBACK_NOT_APPLIED) : FALLBACK_NOT_DEFINED;
        registry.counter(INVOCATIONS_TOTAL, methodTag, resultTag, fallbackTag).inc();
    }

    @Override
    public void retryAttempted() {
        registry.counter(RETRY_RETRIES_TOTAL, methodTag).inc();
    }

    @Override
    public void retryValueReturned(boolean retried) {
        registry.counter(RETRY_CALLS_TOTAL, methodTag, retried ? RETRIED_TRUE : RETRIED_FALSE,
                RETRY_RESULT_VALUE_RETURNED).inc();
    }

    @Override
    public void retryExceptionNotRetryable(boolean retried) {
        registry.counter(RETRY_CALLS_TOTAL, methodTag, retried ? RETRIED_TRUE : RETRIED_FALSE,
                RETRY_RESULT_EXCEPTION_NOT_RETRYABLE).inc();
    }

    @Override
    public void retryMaxRetriesReached(boolean retried) {
        registry.counter(RETRY_CALLS_TOTAL, methodTag, retried ? RETRIED_TRUE : RETRIED_FALSE,
                RETRY_RESULT_MAX_RETRIES_REACHED).inc();
    }

    @Override
    public void retryMaxDurationReached(boolean retried) {
        registry.counter(RETRY_CALLS_TOTAL, methodTag, retried ? RETRIED_TRUE : RETRIED_FALSE,
                RETRY_RESULT_MAX_DURATION_REACHED).inc();
    }

    @Override
    public void timeoutFinished(boolean timedOut, long time) {
        registry.counter(TIMEOUT_CALLS_TOTAL, methodTag, timedOut ? TIMED_OUT_TRUE : TIMED_OUT_FALSE).inc();
        registry.histogram(TIMEOUT_EXECUTION_DURATION_METADATA, methodTag).update(time);
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
        registry.counter(CIRCUIT_BREAKER_CALLS_TOTAL, methodTag, circuitBreakerResultTag).inc();
    }

    @Override
    public void circuitBreakerMovedToOpen() {
        registry.counter(CIRCUIT_BREAKER_OPENED_TOTAL, methodTag).inc();
    }

    @Override
    public void registerCircuitBreakerIsClosed(BooleanSupplier supplier) {
        registerGauge(supplier, CIRCUIT_BREAKER_STATE_CURRENT, MetricUnits.NONE, methodTag,
                CIRCUIT_BREAKER_STATE_CLOSED);
    }

    @Override
    public void registerCircuitBreakerIsOpen(BooleanSupplier supplier) {
        registerGauge(supplier, CIRCUIT_BREAKER_STATE_CURRENT, MetricUnits.NONE, methodTag,
                CIRCUIT_BREAKER_STATE_OPEN);
    }

    @Override
    public void registerCircuitBreakerIsHalfOpen(BooleanSupplier supplier) {
        registerGauge(supplier, CIRCUIT_BREAKER_STATE_CURRENT, MetricUnits.NONE, methodTag,
                CIRCUIT_BREAKER_STATE_HALF_OPEN);
    }

    @Override
    public void registerCircuitBreakerTimeSpentInClosed(LongSupplier supplier) {
        registerGauge(supplier, CIRCUIT_BREAKER_STATE_TOTAL, MetricUnits.NANOSECONDS, methodTag,
                CIRCUIT_BREAKER_STATE_CLOSED);
    }

    @Override
    public void registerCircuitBreakerTimeSpentInOpen(LongSupplier supplier) {
        registerGauge(supplier, CIRCUIT_BREAKER_STATE_TOTAL, MetricUnits.NANOSECONDS, methodTag,
                CIRCUIT_BREAKER_STATE_OPEN);
    }

    @Override
    public void registerCircuitBreakerTimeSpentInHalfOpen(LongSupplier supplier) {
        registerGauge(supplier, CIRCUIT_BREAKER_STATE_TOTAL, MetricUnits.NANOSECONDS, methodTag,
                CIRCUIT_BREAKER_STATE_HALF_OPEN);
    }

    @Override
    public void bulkheadDecisionMade(boolean accepted) {
        Tag bulkheadResultTag = accepted ? BULKHEAD_RESULT_ACCEPTED : BULKHEAD_RESULT_REJECTED;
        registry.counter(BULKHEAD_CALLS_TOTAL, methodTag, bulkheadResultTag).inc();
    }

    @Override
    public void registerBulkheadExecutionsRunning(LongSupplier supplier) {
        registerGauge(supplier, BULKHEAD_EXECUTIONS_RUNNING, MetricUnits.NONE, methodTag);
    }

    @Override
    public void registerBulkheadExecutionsWaiting(LongSupplier supplier) {
        registerGauge(supplier, BULKHEAD_EXECUTIONS_WAITING, MetricUnits.NONE, methodTag);
    }

    @Override
    public void updateBulkheadRunningDuration(long time) {
        registry.histogram(BULKHEAD_RUNNING_DURATION_METADATA, methodTag).update(time);
    }

    @Override
    public void updateBulkheadWaitingDuration(long time) {
        registry.histogram(BULKHEAD_WAITING_DURATION_METADATA, methodTag).update(time);
    }

    @Override
    public void rateLimitDecisionMade(boolean permitted) {
        Tag rateLimitResultTag = permitted ? RATE_LIMIT_RESULT_PERMITTED : RATE_LIMIT_RESULT_REJECTED;
        registry.counter(RATE_LIMIT_CALLS_TOTAL, methodTag, rateLimitResultTag).inc();
    }
}
