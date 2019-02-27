package io.smallrye.faulttolerance.metrics;

import java.lang.reflect.Method;

public final class MetricNames {

    private MetricNames() {
    }

    public static String metricsPrefix(Method method) {
        return "ft." + method.getDeclaringClass().getCanonicalName() + "." + method.getName();
    }

    static final String INVOCATIONS_TOTAL = ".invocations.total";
    static final String INVOCATIONS_FAILED_TOTAL = ".invocations.failed.total";

    public static final String RETRY_RETRIES_TOTAL = ".retry.retries.total";
    public static final String RETRY_CALLS_SUCCEEDED_RETRIED_TOTAL = ".retry.callsSucceededRetried.total";
    public static final String RETRY_CALLS_SUCCEEDED_NOT_RETRIED_TOTAL = ".retry.callsSucceededNotRetried.total";
    public static final String RETRY_CALLS_FAILED_TOTAL = ".retry.callsFailed.total";

    static final String TIMEOUT_CALLS_NOT_TIMED_OUT_TOTAL = ".timeout.callsNotTimedOut.total";
    static final String TIMEOUT_CALLS_TIMED_OUT_TOTAL = ".timeout.callsTimedOut.total";
    static final String TIMEOUT_EXECUTION_DURATION = ".timeout.executionDuration";

    static final String CB_CALLS_SUCCEEDED_TOTAL = ".circuitbreaker.callsSucceeded.total";
    static final String CB_CALLS_PREVENTED_TOTAL =".circuitbreaker.callsPrevented.total";
    static final String CB_CALLS_FAILED_TOTAL = ".circuitbreaker.callsFailed.total";
    static final String CB_OPENED_TOTAL = ".circuitbreaker.opened.total";
    static final String CB_OPEN_TOTAL = ".circuitbreaker.open.total";
    static final String CB_HALF_OPEN_TOTAL = ".circuitbreaker.halfOpen.total";
    static final String CB_CLOSED_TOTAL = ".circuitbreaker.closed.total";

    static final String BULKHEAD_CONCURRENT_EXECUTIONS = ".bulkhead.concurrentExecutions";
    static final String BULKHEAD_CALLS_ACCEPTED_TOTAL = ".bulkhead.callsAccepted.total";
    static final String BULKHEAD_WAITING_QUEUE_POPULATION = ".bulkhead.waitingQueue.population";
    static final String BULKHEAD_CALLS_REJECTED_TOTAL = ".bulkhead.callsRejected.total";
    static final String BULKHEAD_EXECUTION_DURATION = ".bulkhead.executionDuration";
    static final String BULKHEAD_WAITING_DURATION = ".bulkhead.waiting.duration";

    static final String FALLBACK_CALLS_TOTAL = ".fallback.calls.total";

}
