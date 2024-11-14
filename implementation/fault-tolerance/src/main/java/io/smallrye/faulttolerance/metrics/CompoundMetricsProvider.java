package io.smallrye.faulttolerance.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerEvents;
import io.smallrye.faulttolerance.core.metrics.MeteredOperation;
import io.smallrye.faulttolerance.core.metrics.MetricsProvider;
import io.smallrye.faulttolerance.core.metrics.MetricsRecorder;

@Singleton
@Alternative
@Priority(1)
public class CompoundMetricsProvider implements MetricsProvider {
    private final boolean metricsEnabled;

    private final MetricsProvider[] providers;

    private final Map<Object, MetricsRecorder> cache = new ConcurrentHashMap<>();

    @Inject
    CompoundMetricsProvider(
            Instance<MetricsProvider> lookup,
            @ConfigProperty(name = "MP_Fault_Tolerance_Metrics_Enabled", defaultValue = "true") boolean metricsEnabled) {
        List<Class<? extends MetricsProvider>> allProviders = List.of(MicroProfileMetricsProvider.class,
                OpenTelemetryProvider.class, MicrometerProvider.class);

        List<MetricsProvider> providers = new ArrayList<>();
        for (Class<? extends MetricsProvider> clazz : allProviders) {
            try {
                providers.add(lookup.select(clazz).get());
            } catch (Exception ignored) {
                // either the bean does not exist, or some of its dependencies does not exist
            }
        }
        this.metricsEnabled = providers.isEmpty() ? false : metricsEnabled;
        this.providers = providers.toArray(new MetricsProvider[0]);
    }

    @Override
    public boolean isEnabled() {
        return metricsEnabled;
    }

    @Override
    public MetricsRecorder create(MeteredOperation operation) {
        if (metricsEnabled) {
            return cache.computeIfAbsent(operation.cacheKey(), ignored -> {
                if (providers.length == 1) {
                    return providers[0].create(operation);
                }

                MetricsRecorder[] recorders = new MetricsRecorder[providers.length];
                for (int i = 0; i < providers.length; i++) {
                    recorders[i] = providers[i].create(operation);
                }
                return new CompoundMetricsRecorder(recorders);
            });
        } else {
            return MetricsRecorder.NOOP;
        }
    }

    private static class CompoundMetricsRecorder implements MetricsRecorder {
        private final MetricsRecorder[] recorders;

        private CompoundMetricsRecorder(MetricsRecorder... recorders) {
            this.recorders = recorders;
        }

        @Override
        public void executionFinished(boolean succeeded, boolean fallbackDefined, boolean fallbackApplied) {
            for (MetricsRecorder recorder : recorders) {
                recorder.executionFinished(succeeded, fallbackDefined, fallbackApplied);
            }
        }

        @Override
        public void retryAttempted() {
            for (MetricsRecorder recorder : recorders) {
                recorder.retryAttempted();
            }
        }

        @Override
        public void retryValueReturned(boolean retried) {
            for (MetricsRecorder recorder : recorders) {
                recorder.retryValueReturned(retried);
            }
        }

        @Override
        public void retryExceptionNotRetryable(boolean retried) {
            for (MetricsRecorder recorder : recorders) {
                recorder.retryExceptionNotRetryable(retried);
            }
        }

        @Override
        public void retryMaxRetriesReached(boolean retried) {
            for (MetricsRecorder recorder : recorders) {
                recorder.retryMaxRetriesReached(retried);
            }
        }

        @Override
        public void retryMaxDurationReached(boolean retried) {
            for (MetricsRecorder recorder : recorders) {
                recorder.retryMaxDurationReached(retried);
            }
        }

        @Override
        public void timeoutFinished(boolean timedOut, long time) {
            for (MetricsRecorder recorder : recorders) {
                recorder.timeoutFinished(timedOut, time);
            }
        }

        @Override
        public void circuitBreakerFinished(CircuitBreakerEvents.Result result) {
            for (MetricsRecorder recorder : recorders) {
                recorder.circuitBreakerFinished(result);
            }
        }

        @Override
        public void circuitBreakerMovedToOpen() {
            for (MetricsRecorder recorder : recorders) {
                recorder.circuitBreakerMovedToOpen();
            }
        }

        @Override
        public void registerCircuitBreakerIsClosed(BooleanSupplier supplier) {
            for (MetricsRecorder recorder : recorders) {
                recorder.registerCircuitBreakerIsClosed(supplier);
            }
        }

        @Override
        public void registerCircuitBreakerIsOpen(BooleanSupplier supplier) {
            for (MetricsRecorder recorder : recorders) {
                recorder.registerCircuitBreakerIsOpen(supplier);
            }
        }

        @Override
        public void registerCircuitBreakerIsHalfOpen(BooleanSupplier supplier) {
            for (MetricsRecorder recorder : recorders) {
                recorder.registerCircuitBreakerIsHalfOpen(supplier);
            }
        }

        @Override
        public void registerCircuitBreakerTimeSpentInClosed(LongSupplier supplier) {
            for (MetricsRecorder recorder : recorders) {
                recorder.registerCircuitBreakerTimeSpentInClosed(supplier);
            }
        }

        @Override
        public void registerCircuitBreakerTimeSpentInOpen(LongSupplier supplier) {
            for (MetricsRecorder recorder : recorders) {
                recorder.registerCircuitBreakerTimeSpentInOpen(supplier);
            }
        }

        @Override
        public void registerCircuitBreakerTimeSpentInHalfOpen(LongSupplier supplier) {
            for (MetricsRecorder recorder : recorders) {
                recorder.registerCircuitBreakerTimeSpentInHalfOpen(supplier);
            }
        }

        @Override
        public void bulkheadDecisionMade(boolean accepted) {
            for (MetricsRecorder recorder : recorders) {
                recorder.bulkheadDecisionMade(accepted);
            }
        }

        @Override
        public void registerBulkheadExecutionsRunning(LongSupplier supplier) {
            for (MetricsRecorder recorder : recorders) {
                recorder.registerBulkheadExecutionsRunning(supplier);
            }
        }

        @Override
        public void registerBulkheadExecutionsWaiting(LongSupplier supplier) {
            for (MetricsRecorder recorder : recorders) {
                recorder.registerBulkheadExecutionsWaiting(supplier);
            }
        }

        @Override
        public void updateBulkheadRunningDuration(long time) {
            for (MetricsRecorder recorder : recorders) {
                recorder.updateBulkheadRunningDuration(time);
            }
        }

        @Override
        public void updateBulkheadWaitingDuration(long time) {
            for (MetricsRecorder recorder : recorders) {
                recorder.updateBulkheadWaitingDuration(time);
            }
        }

        @Override
        public void rateLimitDecisionMade(boolean permitted) {
            for (MetricsRecorder recorder : recorders) {
                recorder.rateLimitDecisionMade(permitted);
            }
        }
    }
}
