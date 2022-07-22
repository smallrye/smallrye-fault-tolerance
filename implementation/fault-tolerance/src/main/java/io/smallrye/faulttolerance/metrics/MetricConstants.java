package io.smallrye.faulttolerance.metrics;

final class MetricConstants {
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

    static final String RATE_LIMIT_CALLS_TOTAL = "ft.ratelimit.calls.total";
}
