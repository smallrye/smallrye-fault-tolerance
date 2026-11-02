package io.smallrye.faulttolerance.core.metrics;

import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.BULKHEAD_CALLS_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.BULKHEAD_CONFIG_LIMIT;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.BULKHEAD_CONFIG_QUEUE_SIZE;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.BULKHEAD_EXECUTIONS_RUNNING;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.BULKHEAD_EXECUTIONS_WAITING;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.BULKHEAD_RUNNING_DURATION;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.BULKHEAD_WAITING_DURATION;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.CIRCUIT_BREAKER_CALLS_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.CIRCUIT_BREAKER_CONFIG_DELAY;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.CIRCUIT_BREAKER_CONFIG_FAILURE_RATIO;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.CIRCUIT_BREAKER_CONFIG_REQUEST_VOLUME_THRESHOLD;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.CIRCUIT_BREAKER_CONFIG_SUCCESS_THRESHOLD;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.CIRCUIT_BREAKER_OPENED_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.CIRCUIT_BREAKER_STATE_CURRENT;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.CIRCUIT_BREAKER_STATE_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.INVOCATIONS_DURATION;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.INVOCATIONS_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.RATE_LIMIT_CALLS_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.RATE_LIMIT_CONFIG_LIMIT;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.RATE_LIMIT_CONFIG_MIN_SPACING;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.RATE_LIMIT_CONFIG_WINDOW;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.RETRY_CALLS_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.RETRY_CONFIG_MAX_DURATION;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.RETRY_CONFIG_MAX_RETRIES;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.RETRY_RETRIES_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.TIMEOUT_CALLS_TOTAL;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.TIMEOUT_CONFIG_DURATION;
import static io.smallrye.faulttolerance.core.metrics.MetricsConstants.TIMEOUT_EXECUTION_DURATION;

import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
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
    private final Tags methodTagSingleton;

    // invocation
    // when fallback is defined, the `*FallbackApplied` and `*FallbackNotApplied` variants are non-null
    // when fallback is NOT defined, the `*FallbackNotDefined` variants are non-null
    private final Counter invocationValueReturnedFallbackApplied;
    private final Counter invocationExceptionThrownFallbackApplied;
    private final Counter invocationValueReturnedFallbackNotApplied;
    private final Counter invocationExceptionThrownFallbackNotApplied;
    private final Counter invocationValueReturnedFallbackNotDefined;
    private final Counter invocationExceptionThrownFallbackNotDefined;
    private final Timer invocationDuration;

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

        if (operation.hasFallback()) {
            invocationValueReturnedFallbackApplied = registry.counter(INVOCATIONS_TOTAL,
                    Tags.of(methodTag, RESULT_VALUE_RETURNED, FALLBACK_APPLIED));
            invocationExceptionThrownFallbackApplied = registry.counter(INVOCATIONS_TOTAL,
                    Tags.of(methodTag, RESULT_EXCEPTION_THROWN, FALLBACK_APPLIED));
            invocationValueReturnedFallbackNotApplied = registry.counter(INVOCATIONS_TOTAL,
                    Tags.of(methodTag, RESULT_VALUE_RETURNED, FALLBACK_NOT_APPLIED));
            invocationExceptionThrownFallbackNotApplied = registry.counter(INVOCATIONS_TOTAL,
                    Tags.of(methodTag, RESULT_EXCEPTION_THROWN, FALLBACK_NOT_APPLIED));
            invocationValueReturnedFallbackNotDefined = null;
            invocationExceptionThrownFallbackNotDefined = null;
        } else {
            invocationValueReturnedFallbackApplied = null;
            invocationExceptionThrownFallbackApplied = null;
            invocationValueReturnedFallbackNotApplied = null;
            invocationExceptionThrownFallbackNotApplied = null;
            invocationValueReturnedFallbackNotDefined = registry.counter(INVOCATIONS_TOTAL,
                    Tags.of(methodTag, RESULT_VALUE_RETURNED, FALLBACK_NOT_DEFINED));
            invocationExceptionThrownFallbackNotDefined = registry.counter(INVOCATIONS_TOTAL,
                    Tags.of(methodTag, RESULT_EXCEPTION_THROWN, FALLBACK_NOT_DEFINED));
        }
        invocationDuration = registry.timer(INVOCATIONS_DURATION, methodTagSingleton);

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

            registerGauge(operation::retryMaxRetries, RETRY_CONFIG_MAX_RETRIES, methodTagSingleton);
            registerTimeGauge(operation::retryMaxDuration, RETRY_CONFIG_MAX_DURATION, methodTagSingleton);
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

            registerTimeGauge(operation::timeoutDuration, TIMEOUT_CONFIG_DURATION, methodTagSingleton);
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

            registerTimeGauge(operation::circuitBreakerDelay, CIRCUIT_BREAKER_CONFIG_DELAY,
                    methodTagSingleton);
            registerGauge(operation::circuitBreakerRequestVolumeThreshold, CIRCUIT_BREAKER_CONFIG_REQUEST_VOLUME_THRESHOLD,
                    methodTagSingleton);
            registerGauge(operation::circuitBreakerFailureRatio, CIRCUIT_BREAKER_CONFIG_FAILURE_RATIO,
                    methodTagSingleton);
            registerGauge(operation::circuitBreakerSuccessThreshold, CIRCUIT_BREAKER_CONFIG_SUCCESS_THRESHOLD,
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

            registerGauge(operation::bulkheadLimit, BULKHEAD_CONFIG_LIMIT, methodTagSingleton);
            if (operation.mayBeAsynchronous()) {
                registerGauge(operation::bulkheadQueueSize, BULKHEAD_CONFIG_QUEUE_SIZE, methodTagSingleton);
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

            registerGauge(operation::rateLimitLimit, RATE_LIMIT_CONFIG_LIMIT, methodTagSingleton);
            registerTimeGauge(operation::rateLimitWindow, RATE_LIMIT_CONFIG_WINDOW, methodTagSingleton);
            registerTimeGauge(operation::rateLimitMinSpacing, RATE_LIMIT_CONFIG_MIN_SPACING, methodTagSingleton);
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

    private void registerGauge(DoubleSupplier supplier, String name, Tags tags) {
        registry.gauge(name, tags, supplier, ignored -> supplier.getAsDouble());
    }

    private void registerTimeGauge(LongSupplier supplier, String name, Tags tags) {
        registry.more().timeGauge(name, tags, supplier, TimeUnit.NANOSECONDS,
                ignored -> (double) supplier.getAsLong());
    }

    // ---

    @Override
    public void executionFinished(boolean succeeded, boolean fallbackDefined, boolean fallbackApplied, long duration) {
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
        invocationDuration.record(duration, TimeUnit.NANOSECONDS);
    }

    @Override
    public void retryAttempted() {
        retryRetriesTotal.increment();
    }

    @Override
    public void retryValueReturned(boolean retried) {
        (retried ? retryRetriedValueReturned : retryNotRetriedValueReturned).increment();
    }

    @Override
    public void retryExceptionNotRetryable(boolean retried) {
        (retried ? retryRetriedExceptionNotRetryable : retryNotRetriedExceptionNotRetryable).increment();
    }

    @Override
    public void retryMaxRetriesReached(boolean retried) {
        (retried ? retryRetriedMaxRetriesReached : retryNotRetriedMaxRetriesReached).increment();
    }

    @Override
    public void retryMaxDurationReached(boolean retried) {
        (retried ? retryRetriedMaxDurationReached : retryNotRetriedMaxDurationReached).increment();
    }

    @Override
    public void timeoutFinished(boolean timedOut, long time) {
        (timedOut ? timeoutTimedOutTrue : timeoutTimedOutFalse).increment();
        timeoutExecutionDuration.record(time, TimeUnit.NANOSECONDS);
    }

    @Override
    public void circuitBreakerFinished(CircuitBreakerEvents.Result result) {
        switch (result) {
            case SUCCESS -> circuitBreakerSuccess.increment();
            case FAILURE -> circuitBreakerFailure.increment();
            case PREVENTED -> circuitBreakerPrevented.increment();
        }
    }

    @Override
    public void circuitBreakerMovedToOpen() {
        circuitBreakerOpenedTotal.increment();
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
        (accepted ? bulkheadAccepted : bulkheadRejected).increment();
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
        bulkheadRunningDuration.record(time, TimeUnit.NANOSECONDS);
    }

    @Override
    public void updateBulkheadWaitingDuration(long time) {
        bulkheadWaitingDuration.record(time, TimeUnit.NANOSECONDS);
    }

    @Override
    public void rateLimitDecisionMade(boolean permitted) {
        (permitted ? rateLimitPermitted : rateLimitRejected).increment();
    }
}
