package io.smallrye.faulttolerance.metrics;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;

public final class MetricConstants {
    private MetricConstants() {
        // avoid instantiation
    }

    static final String INVOCATIONS_TOTAL = "ft.invocations.total";

    static final String RETRY_CALLS_TOTAL = "ft.retry.calls.total";
    static final String RETRY_RETRIES_TOTAL = "ft.retry.retries.total";

    static final String TIMEOUT_CALLS_TOTAL = "ft.timeout.calls.total";
    static final String TIMEOUT_EXECUTION_DURATION = "ft.timeout.executionDuration";

    static final String CIRCUIT_BREAKER_CALLS_TOTAL = "ft.circuitbreaker.calls.total";
    static final String CIRCUIT_BREAKER_STATE_TOTAL = "ft.circuitbreaker.state.total";
    static final String CIRCUIT_BREAKER_OPENED_TOTAL = "ft.circuitbreaker.opened.total";

    static final String BULKHEAD_CALLS_TOTAL = "ft.bulkhead.calls.total";
    static final String BULKHEAD_EXECUTIONS_RUNNING = "ft.bulkhead.executionsRunning";
    static final String BULKHEAD_EXECUTIONS_WAITING = "ft.bulkhead.executionsWaiting";
    static final String BULKHEAD_RUNNING_DURATION = "ft.bulkhead.runningDuration";
    static final String BULKHEAD_WAITING_DURATION = "ft.bulkhead.waitingDuration";

    static final Metadata TIMEOUT_EXECUTION_DURATION_METADATA = Metadata.builder()
            .withName(TIMEOUT_EXECUTION_DURATION)
            .withType(MetricType.HISTOGRAM)
            .withUnit(MetricUnits.NANOSECONDS)
            .build();

    static final Metadata BULKHEAD_RUNNING_DURATION_METADATA = Metadata.builder()
            .withName(BULKHEAD_RUNNING_DURATION)
            .withType(MetricType.HISTOGRAM)
            .withUnit(MetricUnits.NANOSECONDS)
            .build();

    static final Metadata BULKHEAD_WAITING_DURATION_METADATA = Metadata.builder()
            .withName(BULKHEAD_WAITING_DURATION)
            .withType(MetricType.HISTOGRAM)
            .withUnit(MetricUnits.NANOSECONDS)
            .build();

    // ---

    static final Tag RESULT_VALUE_RETURNED = new Tag("result", "valueReturned");
    static final Tag RESULT_EXCEPTION_THROWN = new Tag("result", "exceptionThrown");

    static final Tag FALLBACK_APPLIED = new Tag("fallback", "applied");
    static final Tag FALLBACK_NOT_APPLIED = new Tag("fallback", "notApplied");
    static final Tag FALLBACK_NOT_DEFINED = new Tag("fallback", "notDefined");

    static final Tag RETRIED_TRUE = new Tag("retried", "true");
    static final Tag RETRIED_FALSE = new Tag("retried", "false");
    static final Tag RETRY_RESULT_VALUE_RETURNED = new Tag("retryResult", "valueReturned");
    static final Tag RETRY_RESULT_EXCEPTION_NOT_RETRYABLE = new Tag("retryResult", "exceptionNotRetryable");
    static final Tag RETRY_RESULT_MAX_RETRIES_REACHED = new Tag("retryResult", "maxRetriesReached");
    static final Tag RETRY_RESULT_MAX_DURATION_REACHED = new Tag("retryResult", "maxDurationReached");

    static final Tag TIMED_OUT_TRUE = new Tag("timedOut", "true");
    static final Tag TIMED_OUT_FALSE = new Tag("timedOut", "false");

    static final Tag CIRCUIT_BREAKER_RESULT_SUCCESS = new Tag("circuitBreakerResult", "success");
    static final Tag CIRCUIT_BREAKER_RESULT_FAILURE = new Tag("circuitBreakerResult", "failure");
    static final Tag CIRCUIT_BREAKER_RESULT_CB_OPEN = new Tag("circuitBreakerResult", "circuitBreakerOpen");

    static final Tag CIRCUIT_BREAKER_STATE_CLOSED = new Tag("state", "closed");
    static final Tag CIRCUIT_BREAKER_STATE_OPEN = new Tag("state", "open");
    static final Tag CIRCUIT_BREAKER_STATE_HALF_OPEN = new Tag("state", "halfOpen");

    static final Tag BULKHEAD_RESULT_ACCEPTED = new Tag("bulkheadResult", "accepted");
    static final Tag BULKHEAD_RESULT_REJECTED = new Tag("bulkheadResult", "rejected");
}
