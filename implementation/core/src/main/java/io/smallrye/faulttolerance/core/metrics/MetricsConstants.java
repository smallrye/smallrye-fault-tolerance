package io.smallrye.faulttolerance.core.metrics;

public final class MetricsConstants {
    private MetricsConstants() {
        // avoid instantiation
    }

    public static final String INVOCATIONS_TOTAL = "ft.invocations.total";

    public static final String RETRY_CALLS_TOTAL = "ft.retry.calls.total";
    public static final String RETRY_RETRIES_TOTAL = "ft.retry.retries.total";
    public static final String RETRY_CONFIG_MAX_RETRIES = "ft.retry.config.maxRetries";
    public static final String RETRY_CONFIG_MAX_DURATION = "ft.retry.config.maxDuration";

    public static final String TIMEOUT_CALLS_TOTAL = "ft.timeout.calls.total";
    public static final String TIMEOUT_EXECUTION_DURATION = "ft.timeout.executionDuration";
    public static final String TIMEOUT_CONFIG_DURATION = "ft.timeout.config.duration";

    public static final String CIRCUIT_BREAKER_CALLS_TOTAL = "ft.circuitbreaker.calls.total";
    public static final String CIRCUIT_BREAKER_STATE_CURRENT = "ft.circuitbreaker.state.current";
    public static final String CIRCUIT_BREAKER_STATE_TOTAL = "ft.circuitbreaker.state.total";
    public static final String CIRCUIT_BREAKER_OPENED_TOTAL = "ft.circuitbreaker.opened.total";
    public static final String CIRCUIT_BREAKER_CONFIG_DELAY = "ft.circuitbreaker.config.delay";
    public static final String CIRCUIT_BREAKER_CONFIG_REQUEST_VOLUME_THRESHOLD = "ft.circuitbreaker.config.requestVolumeThreshold";
    public static final String CIRCUIT_BREAKER_CONFIG_FAILURE_RATIO = "ft.circuitbreaker.config.failureRatio";
    public static final String CIRCUIT_BREAKER_CONFIG_SUCCESS_THRESHOLD = "ft.circuitbreaker.config.successThreshold";

    public static final String BULKHEAD_CALLS_TOTAL = "ft.bulkhead.calls.total";
    public static final String BULKHEAD_EXECUTIONS_RUNNING = "ft.bulkhead.executionsRunning";
    public static final String BULKHEAD_EXECUTIONS_WAITING = "ft.bulkhead.executionsWaiting";
    public static final String BULKHEAD_RUNNING_DURATION = "ft.bulkhead.runningDuration";
    public static final String BULKHEAD_WAITING_DURATION = "ft.bulkhead.waitingDuration";
    public static final String BULKHEAD_CONFIG_LIMIT = "ft.bulkhead.config.limit";
    public static final String BULKHEAD_CONFIG_QUEUE_SIZE = "ft.bulkhead.config.queueSize";

    public static final String RATE_LIMIT_CALLS_TOTAL = "ft.ratelimit.calls.total";
    public static final String RATE_LIMIT_CONFIG_LIMIT = "ft.ratelimit.config.limit";
    public static final String RATE_LIMIT_CONFIG_WINDOW = "ft.ratelimit.config.window";
    public static final String RATE_LIMIT_CONFIG_MIN_SPACING = "ft.ratelimit.config.minSpacing";

    public static final String TIMER_SCHEDULED = "ft.timer.scheduled";
}
