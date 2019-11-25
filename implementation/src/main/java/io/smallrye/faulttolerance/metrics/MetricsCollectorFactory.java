package io.smallrye.faulttolerance.metrics;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.jboss.logging.Logger;

import io.smallrye.faulttolerance.config.FaultToleranceOperation;

@ApplicationScoped
public class MetricsCollectorFactory {

    private static final Logger LOGGER = Logger.getLogger(MetricsCollectorFactory.class);

    @Inject
    MetricRegistry registry;

    @Inject
    @ConfigProperty(name = "MP_Fault_Tolerance_Metrics_Enabled", defaultValue = "true")
    Boolean metricsEnabled;

    public MetricsCollector createCollector(FaultToleranceOperation operation) {
        if (metricsEnabled) {
            return new MetricsCollectorImpl(operation);
        } else {
            return null;
        }
    }

    /**
     * TODO: SmallRye MetricRegistry is not thread-safe and so we synchronize on FaultToleranceOperation
     */
    class MetricsCollectorImpl implements MetricsCollector {

        private final String metricsPrefix;
        private final FaultToleranceOperation operation;

        private volatile AtomicLong cbOpenTotal = new AtomicLong(0);
        private volatile AtomicLong cbClosedTotal = new AtomicLong(0);
        private volatile AtomicLong cbHalfOpenTotal = new AtomicLong(0);

        private volatile AtomicLong bulkheadQueueSize = new AtomicLong(0);
        private volatile AtomicLong bulkheadConcurrentExecutions = new AtomicLong(0);

        MetricsCollectorImpl(FaultToleranceOperation operation) {
            this.metricsPrefix = MetricNames.metricsPrefix(operation.getMethod());
            this.operation = operation;
            runSafely(() -> {
                if (operation.hasCircuitBreaker()) {
                    gaugeRegister(metricsPrefix + MetricNames.CB_OPEN_TOTAL, cbOpenTotal::get);
                    gaugeRegister(metricsPrefix + MetricNames.CB_CLOSED_TOTAL, cbClosedTotal::get);
                    gaugeRegister(metricsPrefix + MetricNames.CB_HALF_OPEN_TOTAL, cbHalfOpenTotal::get);
                }
                if (operation.hasBulkhead()) {
                    if (operation.isAsync()) {
                        gaugeRegister(metricsPrefix + MetricNames.BULKHEAD_WAITING_QUEUE_POPULATION,
                                bulkheadQueueSize::get);
                    }
                    gaugeRegister(metricsPrefix + MetricNames.BULKHEAD_CONCURRENT_EXECUTIONS,
                            bulkheadConcurrentExecutions::get);
                }
            });
        }

        private void counterInc(String name) {
            counterOf(name).inc();
        }

        private void gaugeRegister(String name, Supplier<Long> supplier) {
            MetricID metricID = new MetricID(name);
            Gauge<?> gauge = registry.getGauges().get(metricID);
            if (gauge == null) {
                synchronized (operation) {
                    gauge = registry.getGauges().get(metricID);
                    if (gauge == null) {
                        registry.register(name, (Gauge<Long>) supplier::get);
                    }
                }
            }
        }

        private void histogramUpdate(String name, long value) {
            histogramOf(name).update(value);
        }

        private Counter counterOf(String name) {
            MetricID metricID = new MetricID(name);
            Counter counter = registry.getCounters().get(metricID);
            if (counter == null) {
                synchronized (operation) {
                    counter = registry.getCounters().get(metricID);
                    if (counter == null) {
                        counter = registry.counter(metadataOf(name, MetricType.COUNTER));
                    }
                }
            }
            return counter;
        }

        private Histogram histogramOf(String name) {
            MetricID metricID = new MetricID(name);
            Histogram histogram = registry.getHistograms().get(metricID);
            if (histogram == null) {
                synchronized (operation) {
                    histogram = registry.getHistograms().get(metricID);
                    if (histogram == null) {
                        histogram = registry.histogram(metadataOf(name, MetricType.HISTOGRAM));
                    }
                }
            }
            return histogram;
        }

        @Override
        public void bulkheadQueueEntered() {
            bulkheadQueueSize.incrementAndGet();
        }

        @Override
        public void bulkheadEntered(long timeInQueue) {
            if (timeInQueue > 0) {
                histogramUpdate(metricsPrefix + MetricNames.BULKHEAD_WAITING_DURATION, timeInQueue);
            }
            bulkheadQueueSize.decrementAndGet();
            bulkheadConcurrentExecutions.incrementAndGet();
            counterInc(metricsPrefix + MetricNames.BULKHEAD_CALLS_ACCEPTED_TOTAL);
        }

        @Override
        public void bulkheadRejected() {
            counterInc(metricsPrefix + MetricNames.BULKHEAD_CALLS_REJECTED_TOTAL);
        }

        @Override
        public void bulkheadLeft(long processingTime) {
            bulkheadConcurrentExecutions.decrementAndGet();
            histogramUpdate(metricsPrefix + MetricNames.BULKHEAD_EXECUTION_DURATION, processingTime);
        }

        @Override
        public void circuitBreakerRejected() {
            counterInc(metricsPrefix + MetricNames.CB_CALLS_PREVENTED_TOTAL);
        }

        @Override
        public void circuitBreakerOpenTime(long time) {
            cbOpenTotal.addAndGet(time);
        }

        @Override
        public void circuitBreakerHalfOpenTime(long time) {
            cbHalfOpenTotal.addAndGet(time);
        }

        @Override
        public void circuitBreakerClosedTime(long time) {
            cbClosedTotal.addAndGet(time);
        }

        @Override
        public void circuitBreakerClosedToOpen() {
            counterInc(metricsPrefix + MetricNames.CB_OPENED_TOTAL);
        }

        @Override
        public void circuitBreakerFailed() {
            counterInc(metricsPrefix + MetricNames.CB_CALLS_FAILED_TOTAL);
        }

        @Override
        public void circuitBreakerSucceeded() {
            counterInc(metricsPrefix + MetricNames.CB_CALLS_SUCCEEDED_TOTAL);
        }

        @Override
        public void fallbackCalled() {
            counterInc(metricsPrefix + MetricNames.FALLBACK_CALLS_TOTAL);
        }

        @Override
        public void retrySucceededNotRetried() {
            counterInc(metricsPrefix + MetricNames.RETRY_CALLS_SUCCEEDED_NOT_RETRIED_TOTAL);
        }

        @Override
        public void retrySucceededRetried() {
            counterInc(metricsPrefix + MetricNames.RETRY_CALLS_SUCCEEDED_RETRIED_TOTAL);
        }

        @Override
        public void retryFailed() {
            counterInc(metricsPrefix + MetricNames.RETRY_CALLS_FAILED_TOTAL);
        }

        @Override
        public void retryRetried() {
            counterInc(metricsPrefix + MetricNames.RETRY_RETRIES_TOTAL);
        }

        @Override
        public void timeoutSucceeded(long time) {
            histogramUpdate(metricsPrefix + MetricNames.TIMEOUT_EXECUTION_DURATION, time);
            counterInc(metricsPrefix + MetricNames.TIMEOUT_CALLS_NOT_TIMED_OUT_TOTAL);
        }

        @Override
        public void timeoutTimedOut(long time) {
            counterInc(metricsPrefix + MetricNames.TIMEOUT_CALLS_TIMED_OUT_TOTAL);
        }

        @Override
        public void invoked() {
            counterInc(metricsPrefix + MetricNames.INVOCATIONS_TOTAL);
        }

        @Override
        public void failed() {
            counterInc(metricsPrefix + MetricNames.INVOCATIONS_FAILED_TOTAL);
        }
    }

    private void runSafely(Runnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException any) {
            LOGGER.warn("Collecting metrics failed", any);
        }
    }

    public static Metadata metadataOf(String name, MetricType metricType) {
        return Metadata.builder()
                .withName(name)
                .withType(metricType)
                .reusable()
                .build();
    }

}
