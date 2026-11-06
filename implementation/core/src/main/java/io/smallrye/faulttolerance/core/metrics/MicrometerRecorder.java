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

import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerEvents;

public class MicrometerRecorder implements MetricsRecorder {
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

    private final MeterRegistry registry;
    private final Tag methodTag;
    private final Tags methodTagSingleton;

    // invocation
    private final Counter invocationValueReturnedFallbackApplied;
    private final Counter invocationExceptionThrownFallbackApplied;
    private final Counter invocationValueReturnedFallbackNotApplied;
    private final Counter invocationExceptionThrownFallbackNotApplied;
    private final Counter invocationValueReturnedFallbackNotDefined;
    private final Counter invocationExceptionThrownFallbackNotDefined;

    // retry
    private final Counter retryRetriesTotal;
    private final Counter retryRetriedValueReturned;
    private final Counter retryRetriedExceptionNotRetryable;
    private final Counter retryRetriedMaxRetriesReached;
    private final Counter retryRetriedMaxDurationReached;
    private final Counter retryNotRetriedValueReturned;
    private final Counter retryNotRetriedExceptionNotRetryable;
    private final Counter retryNotRetriedMaxRetriesReached;
    private final Counter retryNotRetriedMaxDurationReached;

    // timeout
    private final Counter timeoutTimedOutTrue;
    private final Counter timeoutTimedOutFalse;
    private final Timer timeoutExecutionDuration;

    // circuit breaker
    private final Counter circuitBreakerSuccess;
    private final Counter circuitBreakerFailure;
    private final Counter circuitBreakerPrevented;
    private final Counter circuitBreakerOpenedTotal;

    // bulkhead
    private final Counter bulkheadAccepted;
    private final Counter bulkheadRejected;
    private final Timer bulkheadRunningDuration;
    private final Timer bulkheadWaitingDuration;

    // rate limit
    private final Counter rateLimitPermitted;
    private final Counter rateLimitRejected;

    public MicrometerRecorder(MeterRegistry registry, MeteredOperation operation) {
        this.registry = registry;
        this.methodTag = Tag.of("method", operation.name());
        this.methodTagSingleton = Tags.of(methodTag);

        invocationValueReturnedFallbackApplied = registry.counter(INVOCATIONS_TOTAL,
                Tags.of(methodTag, RESULT_VALUE_RETURNED, FALLBACK_APPLIED));
        invocationExceptionThrownFallbackApplied = registry.counter(INVOCATIONS_TOTAL,
                Tags.of(methodTag, RESULT_EXCEPTION_THROWN, FALLBACK_APPLIED));
        invocationValueReturnedFallbackNotApplied = registry.counter(INVOCATIONS_TOTAL,
                Tags.of(methodTag, RESULT_VALUE_RETURNED, FALLBACK_NOT_APPLIED));
        invocationExceptionThrownFallbackNotApplied = registry.counter(INVOCATIONS_TOTAL,
                Tags.of(methodTag, RESULT_EXCEPTION_THROWN, FALLBACK_NOT_APPLIED));
        invocationValueReturnedFallbackNotDefined = registry.counter(INVOCATIONS_TOTAL,
                Tags.of(methodTag, RESULT_VALUE_RETURNED, FALLBACK_NOT_DEFINED));
        invocationExceptionThrownFallbackNotDefined = registry.counter(INVOCATIONS_TOTAL,
                Tags.of(methodTag, RESULT_EXCEPTION_THROWN, FALLBACK_NOT_DEFINED));

        if (operation.hasRetry()) {
            retryRetriesTotal = registry.counter(RETRY_RETRIES_TOTAL, methodTagSingleton);

            retryRetriedValueReturned = registry.counter(RETRY_CALLS_TOTAL,
                    Tags.of(methodTag, RETRIED_TRUE, RETRY_RESULT_VALUE_RETURNED));
            retryRetriedExceptionNotRetryable = registry.counter(RETRY_CALLS_TOTAL,
                    Tags.of(methodTag, RETRIED_TRUE, RETRY_RESULT_EXCEPTION_NOT_RETRYABLE));
            retryRetriedMaxRetriesReached = registry.counter(RETRY_CALLS_TOTAL,
                    Tags.of(methodTag, RETRIED_TRUE, RETRY_RESULT_MAX_RETRIES_REACHED));
            retryRetriedMaxDurationReached = registry.counter(RETRY_CALLS_TOTAL,
                    Tags.of(methodTag, RETRIED_TRUE, RETRY_RESULT_MAX_DURATION_REACHED));
            retryNotRetriedValueReturned = registry.counter(RETRY_CALLS_TOTAL,
                    Tags.of(methodTag, RETRIED_FALSE, RETRY_RESULT_VALUE_RETURNED));
            retryNotRetriedExceptionNotRetryable = registry.counter(RETRY_CALLS_TOTAL,
                    Tags.of(methodTag, RETRIED_FALSE, RETRY_RESULT_EXCEPTION_NOT_RETRYABLE));
            retryNotRetriedMaxRetriesReached = registry.counter(RETRY_CALLS_TOTAL,
                    Tags.of(methodTag, RETRIED_FALSE, RETRY_RESULT_MAX_RETRIES_REACHED));
            retryNotRetriedMaxDurationReached = registry.counter(RETRY_CALLS_TOTAL,
                    Tags.of(methodTag, RETRIED_FALSE, RETRY_RESULT_MAX_DURATION_REACHED));
        } else {
            retryRetriesTotal = null;
            retryRetriedValueReturned = null;
            retryRetriedExceptionNotRetryable = null;
            retryRetriedMaxRetriesReached = null;
            retryRetriedMaxDurationReached = null;
            retryNotRetriedValueReturned = null;
            retryNotRetriedExceptionNotRetryable = null;
            retryNotRetriedMaxRetriesReached = null;
            retryNotRetriedMaxDurationReached = null;
        }

        if (operation.hasTimeout()) {
            timeoutTimedOutTrue = registry.counter(TIMEOUT_CALLS_TOTAL, Tags.of(methodTag, TIMED_OUT_TRUE));
            timeoutTimedOutFalse = registry.counter(TIMEOUT_CALLS_TOTAL, Tags.of(methodTag, TIMED_OUT_FALSE));
            timeoutExecutionDuration = registry.timer(TIMEOUT_EXECUTION_DURATION, methodTagSingleton);
        } else {
            timeoutTimedOutTrue = null;
            timeoutTimedOutFalse = null;
            timeoutExecutionDuration = null;
        }

        if (operation.hasCircuitBreaker()) {
            circuitBreakerSuccess = registry.counter(CIRCUIT_BREAKER_CALLS_TOTAL,
                    Tags.of(methodTag, CIRCUIT_BREAKER_RESULT_SUCCESS));
            circuitBreakerFailure = registry.counter(CIRCUIT_BREAKER_CALLS_TOTAL,
                    Tags.of(methodTag, CIRCUIT_BREAKER_RESULT_FAILURE));
            circuitBreakerPrevented = registry.counter(CIRCUIT_BREAKER_CALLS_TOTAL,
                    Tags.of(methodTag, CIRCUIT_BREAKER_RESULT_CB_OPEN));
            circuitBreakerOpenedTotal = registry.counter(CIRCUIT_BREAKER_OPENED_TOTAL,
                    methodTagSingleton);
        } else {
            circuitBreakerSuccess = null;
            circuitBreakerFailure = null;
            circuitBreakerPrevented = null;
            circuitBreakerOpenedTotal = null;
        }

        if (operation.hasBulkhead()) {
            bulkheadAccepted = registry.counter(BULKHEAD_CALLS_TOTAL, Tags.of(methodTag, BULKHEAD_RESULT_ACCEPTED));
            bulkheadRejected = registry.counter(BULKHEAD_CALLS_TOTAL, Tags.of(methodTag, BULKHEAD_RESULT_REJECTED));
            bulkheadRunningDuration = registry.timer(BULKHEAD_RUNNING_DURATION, methodTagSingleton);
            if (operation.mayBeAsynchronous()) {
                bulkheadWaitingDuration = registry.timer(BULKHEAD_WAITING_DURATION, methodTagSingleton);
            } else {
                bulkheadWaitingDuration = null;
            }
        } else {
            bulkheadAccepted = null;
            bulkheadRejected = null;
            bulkheadRunningDuration = null;
            bulkheadWaitingDuration = null;
        }

        if (operation.hasRateLimit()) {
            rateLimitPermitted = registry.counter(RATE_LIMIT_CALLS_TOTAL,
                    Tags.of(methodTag, RATE_LIMIT_RESULT_PERMITTED));
            rateLimitRejected = registry.counter(RATE_LIMIT_CALLS_TOTAL,
                    Tags.of(methodTag, RATE_LIMIT_RESULT_REJECTED));
        } else {
            rateLimitPermitted = null;
            rateLimitRejected = null;
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

    private void registerGauge(LongSupplier supplier, String name, Tags tags) {
        registry.gauge(name, tags, supplier, ignored -> (double) supplier.getAsLong());
    }

    private void registerGauge(BooleanSupplier supplier, String name, Tags tags) {
        registry.gauge(name, tags, supplier, ignored -> supplier.getAsBoolean() ? 1.0 : 0.0);
    }

    private void registerTimeGauge(LongSupplier supplier, String name, Tags tags) {
        registry.more().timeGauge(name, tags, supplier, TimeUnit.NANOSECONDS,
                ignored -> (double) supplier.getAsLong());
    }

    // ---

    // the meter fields have a value only if the operation declares the corresponding strategy,
    // but a metrics recorder may be shared by multiple operations with the same cache key and different
    // strategies, so a record method may be called even when the corresponding field is `null`;
    // in that case, the meter is created on demand

    @Override
    public void executionFinished(boolean succeeded, boolean fallbackDefined, boolean fallbackApplied) {
        Counter counter;
        if (succeeded) {
            if (fallbackDefined) {
                counter = fallbackApplied
                        ? invocationValueReturnedFallbackApplied
                        : invocationValueReturnedFallbackNotApplied;
            } else {
                counter = invocationValueReturnedFallbackNotDefined;
            }
        } else {
            if (fallbackDefined) {
                counter = fallbackApplied
                        ? invocationExceptionThrownFallbackApplied
                        : invocationExceptionThrownFallbackNotApplied;
            } else {
                counter = invocationExceptionThrownFallbackNotDefined;
            }
        }
        counter.increment();
    }

    @Override
    public void retryAttempted() {
        Counter counter = retryRetriesTotal;
        if (counter == null) {
            counter = registry.counter(RETRY_RETRIES_TOTAL, methodTagSingleton);
        }
        counter.increment();
    }

    @Override
    public void retryValueReturned(boolean retried) {
        retryCallsTotal(retried ? retryRetriedValueReturned : retryNotRetriedValueReturned,
                retried, RETRY_RESULT_VALUE_RETURNED);
    }

    @Override
    public void retryExceptionNotRetryable(boolean retried) {
        retryCallsTotal(retried ? retryRetriedExceptionNotRetryable : retryNotRetriedExceptionNotRetryable,
                retried, RETRY_RESULT_EXCEPTION_NOT_RETRYABLE);
    }

    @Override
    public void retryMaxRetriesReached(boolean retried) {
        retryCallsTotal(retried ? retryRetriedMaxRetriesReached : retryNotRetriedMaxRetriesReached,
                retried, RETRY_RESULT_MAX_RETRIES_REACHED);
    }

    @Override
    public void retryMaxDurationReached(boolean retried) {
        retryCallsTotal(retried ? retryRetriedMaxDurationReached : retryNotRetriedMaxDurationReached,
                retried, RETRY_RESULT_MAX_DURATION_REACHED);
    }

    private void retryCallsTotal(Counter counter, boolean retried, Tag retryResultTag) {
        if (counter == null) {
            counter = registry.counter(RETRY_CALLS_TOTAL,
                    Tags.of(methodTag, retried ? RETRIED_TRUE : RETRIED_FALSE, retryResultTag));
        }
        counter.increment();
    }

    @Override
    public void timeoutFinished(boolean timedOut, long time) {
        Counter counter = timedOut ? timeoutTimedOutTrue : timeoutTimedOutFalse;
        if (counter == null) {
            counter = registry.counter(TIMEOUT_CALLS_TOTAL,
                    Tags.of(methodTag, timedOut ? TIMED_OUT_TRUE : TIMED_OUT_FALSE));
        }
        counter.increment();

        Timer timer = timeoutExecutionDuration;
        if (timer == null) {
            timer = registry.timer(TIMEOUT_EXECUTION_DURATION, methodTagSingleton);
        }
        timer.record(time, TimeUnit.NANOSECONDS);
    }

    @Override
    public void circuitBreakerFinished(CircuitBreakerEvents.Result result) {
        Counter counter;
        Tag resultTag;
        switch (result) {
            case SUCCESS:
                counter = circuitBreakerSuccess;
                resultTag = CIRCUIT_BREAKER_RESULT_SUCCESS;
                break;
            case FAILURE:
                counter = circuitBreakerFailure;
                resultTag = CIRCUIT_BREAKER_RESULT_FAILURE;
                break;
            case PREVENTED:
                counter = circuitBreakerPrevented;
                resultTag = CIRCUIT_BREAKER_RESULT_CB_OPEN;
                break;
            default:
                throw new IllegalArgumentException("Unknown circuit breaker result: " + result);
        }
        if (counter == null) {
            counter = registry.counter(CIRCUIT_BREAKER_CALLS_TOTAL, Tags.of(methodTag, resultTag));
        }
        counter.increment();
    }

    @Override
    public void circuitBreakerMovedToOpen() {
        Counter counter = circuitBreakerOpenedTotal;
        if (counter == null) {
            counter = registry.counter(CIRCUIT_BREAKER_OPENED_TOTAL, methodTagSingleton);
        }
        counter.increment();
    }

    @Override
    public void registerCircuitBreakerIsClosed(BooleanSupplier supplier) {
        registerGauge(supplier, CIRCUIT_BREAKER_STATE_CURRENT, Tags.of(methodTag, CIRCUIT_BREAKER_STATE_CLOSED));
    }

    @Override
    public void registerCircuitBreakerIsOpen(BooleanSupplier supplier) {
        registerGauge(supplier, CIRCUIT_BREAKER_STATE_CURRENT, Tags.of(methodTag, CIRCUIT_BREAKER_STATE_OPEN));
    }

    @Override
    public void registerCircuitBreakerIsHalfOpen(BooleanSupplier supplier) {
        registerGauge(supplier, CIRCUIT_BREAKER_STATE_CURRENT, Tags.of(methodTag, CIRCUIT_BREAKER_STATE_HALF_OPEN));
    }

    @Override
    public void registerCircuitBreakerTimeSpentInClosed(LongSupplier supplier) {
        registerTimeGauge(supplier, CIRCUIT_BREAKER_STATE_TOTAL, Tags.of(methodTag, CIRCUIT_BREAKER_STATE_CLOSED));
    }

    @Override
    public void registerCircuitBreakerTimeSpentInOpen(LongSupplier supplier) {
        registerTimeGauge(supplier, CIRCUIT_BREAKER_STATE_TOTAL, Tags.of(methodTag, CIRCUIT_BREAKER_STATE_OPEN));
    }

    @Override
    public void registerCircuitBreakerTimeSpentInHalfOpen(LongSupplier supplier) {
        registerTimeGauge(supplier, CIRCUIT_BREAKER_STATE_TOTAL, Tags.of(methodTag, CIRCUIT_BREAKER_STATE_HALF_OPEN));
    }

    @Override
    public void bulkheadDecisionMade(boolean accepted) {
        Counter counter = accepted ? bulkheadAccepted : bulkheadRejected;
        if (counter == null) {
            counter = registry.counter(BULKHEAD_CALLS_TOTAL,
                    Tags.of(methodTag, accepted ? BULKHEAD_RESULT_ACCEPTED : BULKHEAD_RESULT_REJECTED));
        }
        counter.increment();
    }

    @Override
    public void registerBulkheadExecutionsRunning(LongSupplier supplier) {
        registerGauge(supplier, BULKHEAD_EXECUTIONS_RUNNING, methodTagSingleton);
    }

    @Override
    public void registerBulkheadExecutionsWaiting(LongSupplier supplier) {
        registerGauge(supplier, BULKHEAD_EXECUTIONS_WAITING, methodTagSingleton);
    }

    @Override
    public void updateBulkheadRunningDuration(long time) {
        Timer timer = bulkheadRunningDuration;
        if (timer == null) {
            timer = registry.timer(BULKHEAD_RUNNING_DURATION, methodTagSingleton);
        }
        timer.record(time, TimeUnit.NANOSECONDS);
    }

    @Override
    public void updateBulkheadWaitingDuration(long time) {
        Timer timer = bulkheadWaitingDuration;
        if (timer == null) {
            timer = registry.timer(BULKHEAD_WAITING_DURATION, methodTagSingleton);
        }
        timer.record(time, TimeUnit.NANOSECONDS);
    }

    @Override
    public void rateLimitDecisionMade(boolean permitted) {
        Counter counter = permitted ? rateLimitPermitted : rateLimitRejected;
        if (counter == null) {
            counter = registry.counter(RATE_LIMIT_CALLS_TOTAL,
                    Tags.of(methodTag, permitted ? RATE_LIMIT_RESULT_PERMITTED : RATE_LIMIT_RESULT_REJECTED));
        }
        counter.increment();
    }
}
