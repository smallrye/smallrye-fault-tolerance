package io.smallrye.faulttolerance.metrics;

import static io.smallrye.faulttolerance.metrics.MetricConstants.BULKHEAD_CALLS_TOTAL;
import static io.smallrye.faulttolerance.metrics.MetricConstants.BULKHEAD_EXECUTIONS_RUNNING;
import static io.smallrye.faulttolerance.metrics.MetricConstants.BULKHEAD_EXECUTIONS_WAITING;
import static io.smallrye.faulttolerance.metrics.MetricConstants.BULKHEAD_RESULT_ACCEPTED;
import static io.smallrye.faulttolerance.metrics.MetricConstants.BULKHEAD_RESULT_REJECTED;
import static io.smallrye.faulttolerance.metrics.MetricConstants.BULKHEAD_RUNNING_DURATION_METADATA;
import static io.smallrye.faulttolerance.metrics.MetricConstants.BULKHEAD_WAITING_DURATION_METADATA;
import static io.smallrye.faulttolerance.metrics.MetricConstants.CIRCUIT_BREAKER_CALLS_TOTAL;
import static io.smallrye.faulttolerance.metrics.MetricConstants.CIRCUIT_BREAKER_OPENED_TOTAL;
import static io.smallrye.faulttolerance.metrics.MetricConstants.CIRCUIT_BREAKER_RESULT_CB_OPEN;
import static io.smallrye.faulttolerance.metrics.MetricConstants.CIRCUIT_BREAKER_RESULT_FAILURE;
import static io.smallrye.faulttolerance.metrics.MetricConstants.CIRCUIT_BREAKER_RESULT_SUCCESS;
import static io.smallrye.faulttolerance.metrics.MetricConstants.CIRCUIT_BREAKER_STATE_CLOSED;
import static io.smallrye.faulttolerance.metrics.MetricConstants.CIRCUIT_BREAKER_STATE_HALF_OPEN;
import static io.smallrye.faulttolerance.metrics.MetricConstants.CIRCUIT_BREAKER_STATE_OPEN;
import static io.smallrye.faulttolerance.metrics.MetricConstants.CIRCUIT_BREAKER_STATE_TOTAL;
import static io.smallrye.faulttolerance.metrics.MetricConstants.FALLBACK_APPLIED;
import static io.smallrye.faulttolerance.metrics.MetricConstants.FALLBACK_NOT_APPLIED;
import static io.smallrye.faulttolerance.metrics.MetricConstants.FALLBACK_NOT_DEFINED;
import static io.smallrye.faulttolerance.metrics.MetricConstants.INVOCATIONS_TOTAL;
import static io.smallrye.faulttolerance.metrics.MetricConstants.RESULT_EXCEPTION_THROWN;
import static io.smallrye.faulttolerance.metrics.MetricConstants.RESULT_VALUE_RETURNED;
import static io.smallrye.faulttolerance.metrics.MetricConstants.RETRIED_FALSE;
import static io.smallrye.faulttolerance.metrics.MetricConstants.RETRIED_TRUE;
import static io.smallrye.faulttolerance.metrics.MetricConstants.RETRY_CALLS_TOTAL;
import static io.smallrye.faulttolerance.metrics.MetricConstants.RETRY_RESULT_EXCEPTION_NOT_RETRYABLE;
import static io.smallrye.faulttolerance.metrics.MetricConstants.RETRY_RESULT_MAX_DURATION_REACHED;
import static io.smallrye.faulttolerance.metrics.MetricConstants.RETRY_RESULT_MAX_RETRIES_REACHED;
import static io.smallrye.faulttolerance.metrics.MetricConstants.RETRY_RESULT_VALUE_RETURNED;
import static io.smallrye.faulttolerance.metrics.MetricConstants.RETRY_RETRIES_TOTAL;
import static io.smallrye.faulttolerance.metrics.MetricConstants.TIMED_OUT_FALSE;
import static io.smallrye.faulttolerance.metrics.MetricConstants.TIMED_OUT_TRUE;
import static io.smallrye.faulttolerance.metrics.MetricConstants.TIMEOUT_CALLS_TOTAL;
import static io.smallrye.faulttolerance.metrics.MetricConstants.TIMEOUT_EXECUTION_DURATION_METADATA;

import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

import io.smallrye.faulttolerance.SpecCompatibility;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerEvents;
import io.smallrye.faulttolerance.core.metrics.MetricsRecorder;

@ApplicationScoped
public class MetricsProvider {

    // this must be a lazy injection (`Provider<MetricRegistry>` instead of just `MetricRegistry`),
    // because when metrics are disabled, no `MetricRegistry` has to be present
    @Inject
    @RegistryType(type = MetricRegistry.Type.BASE)
    Provider<MetricRegistry> registry;

    @Inject
    @ConfigProperty(name = "MP_Fault_Tolerance_Metrics_Enabled", defaultValue = "true")
    boolean metricsEnabled;

    @Inject
    SpecCompatibility specCompatibility;

    public MetricsRecorder create(FaultToleranceOperation operation) {
        if (metricsEnabled) {
            return new MetricsRecorderImpl(registry.get(), specCompatibility, operation);
        } else {
            return MetricsRecorder.NOOP;
        }
    }

    public boolean isEnabled() {
        return metricsEnabled;
    }

    private static class MetricsRecorderImpl implements MetricsRecorder {
        private final MetricRegistry registry;
        private final Tag methodTag;

        MetricsRecorderImpl(MetricRegistry registry, SpecCompatibility specCompatibility, FaultToleranceOperation operation) {
            this.registry = registry;
            this.methodTag = new Tag("method", operation.getName());

            registerMetrics(specCompatibility, operation);
        }

        private void registerMetrics(SpecCompatibility specCompatibility, FaultToleranceOperation operation) {
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
                if (specCompatibility.isOperationTrulyOrPseudoAsynchronous(operation)) {
                    registry.histogram(BULKHEAD_WAITING_DURATION_METADATA, methodTag).getCount();
                }
            }
        }

        private void registerGauge(Supplier<Long> supplier, String name, String unit, Tag... tags) {
            Metadata metadata = Metadata.builder()
                    .withName(name)
                    .withType(MetricType.GAUGE)
                    .withUnit(unit)
                    .build();
            registry.gauge(metadata, supplier, tags);
        }

        // ---

        @Override
        public void executionFinished(boolean succeeded, boolean fallbackDefined, boolean fallbackApplied) {
            Tag resultTag = succeeded ? RESULT_VALUE_RETURNED : RESULT_EXCEPTION_THROWN;
            Tag fallbackTag = fallbackDefined
                    ? (fallbackApplied ? FALLBACK_APPLIED : FALLBACK_NOT_APPLIED)
                    : FALLBACK_NOT_DEFINED;
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
        public void registerCircuitBreakerTimeSpentInClosed(Supplier<Long> supplier) {
            registerGauge(supplier, CIRCUIT_BREAKER_STATE_TOTAL, MetricUnits.NANOSECONDS,
                    methodTag, CIRCUIT_BREAKER_STATE_CLOSED);
        }

        @Override
        public void registerCircuitBreakerTimeSpentInOpen(Supplier<Long> supplier) {
            registerGauge(supplier, CIRCUIT_BREAKER_STATE_TOTAL, MetricUnits.NANOSECONDS,
                    methodTag, CIRCUIT_BREAKER_STATE_OPEN);
        }

        @Override
        public void registerCircuitBreakerTimeSpentInHalfOpen(Supplier<Long> supplier) {
            registerGauge(supplier, CIRCUIT_BREAKER_STATE_TOTAL, MetricUnits.NANOSECONDS,
                    methodTag, CIRCUIT_BREAKER_STATE_HALF_OPEN);
        }

        @Override
        public void bulkheadDecisionMade(boolean accepted) {
            Tag bulkheadResultTag = accepted ? BULKHEAD_RESULT_ACCEPTED : BULKHEAD_RESULT_REJECTED;
            registry.counter(BULKHEAD_CALLS_TOTAL, methodTag, bulkheadResultTag).inc();
        }

        @Override
        public void registerBulkheadExecutionsRunning(Supplier<Long> supplier) {
            registerGauge(supplier, BULKHEAD_EXECUTIONS_RUNNING, MetricUnits.NONE, methodTag);
        }

        @Override
        public void registerBulkheadExecutionsWaiting(Supplier<Long> supplier) {
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
    }
}
