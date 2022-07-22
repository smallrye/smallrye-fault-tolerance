package io.smallrye.faulttolerance.core.metrics;

import static io.smallrye.faulttolerance.core.metrics.MetricsLogger.LOG;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.bulkhead.BulkheadEvents;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerEvents;
import io.smallrye.faulttolerance.core.fallback.FallbackEvents;
import io.smallrye.faulttolerance.core.rate.limit.RateLimitEvents;
import io.smallrye.faulttolerance.core.retry.RetryEvents;
import io.smallrye.faulttolerance.core.timeout.TimeoutEvents;

public class MetricsCollector<V> implements FaultToleranceStrategy<V> {
    final FaultToleranceStrategy<V> delegate;
    final MetricsRecorder metrics;
    final boolean isAsync;
    final boolean hasBulkhead;
    final boolean hasCircuitBreaker;
    final boolean hasRateLimit;
    final boolean hasRetry;
    final boolean hasTimeout;

    // per-invocation metric values are stored in local variables in the `registerMetrics` method
    // shared metric values (for stateful fault tolerance strategies) are stored in fields below

    // circuit breaker

    private volatile CircuitBreakerState state;
    private final AtomicLong previousHalfOpenTime = new AtomicLong();
    private volatile long halfOpenStart;
    private final AtomicLong previousClosedTime = new AtomicLong();
    private volatile long closedStart;
    private final AtomicLong previousOpenTime = new AtomicLong();
    private volatile long openStart;

    // bulkhead

    private final AtomicLong runningExecutions = new AtomicLong();
    private final AtomicLong waitingExecutions = new AtomicLong();

    public MetricsCollector(FaultToleranceStrategy<V> delegate, MetricsRecorder metrics, boolean isAsync,
            boolean hasBulkhead, boolean hasCircuitBreaker, boolean hasRateLimit, boolean hasRetry, boolean hasTimeout) {
        this.delegate = delegate;
        this.metrics = metrics;
        this.isAsync = isAsync;
        this.hasBulkhead = hasBulkhead;
        this.hasCircuitBreaker = hasCircuitBreaker;
        this.hasRateLimit = hasRateLimit;
        this.hasRetry = hasRetry;
        this.hasTimeout = hasTimeout;

        this.state = CircuitBreakerState.CLOSED;
        this.closedStart = System.nanoTime();

        if (hasCircuitBreaker) {
            metrics.registerCircuitBreakerTimeSpentInClosed(
                    () -> getTime(CircuitBreakerState.CLOSED, closedStart, previousClosedTime));
            metrics.registerCircuitBreakerTimeSpentInOpen(
                    () -> getTime(CircuitBreakerState.OPEN, openStart, previousOpenTime));
            metrics.registerCircuitBreakerTimeSpentInHalfOpen(
                    () -> getTime(CircuitBreakerState.HALF_OPEN, halfOpenStart, previousHalfOpenTime));
        }

        if (hasBulkhead) {
            metrics.registerBulkheadExecutionsRunning(runningExecutions::get);
            if (isAsync) {
                metrics.registerBulkheadExecutionsWaiting(waitingExecutions::get);
            }
        }
    }

    private Long getTime(CircuitBreakerState measuredState, long measuredStateStart, AtomicLong prevMeasuredStateTime) {
        return state == measuredState
                ? prevMeasuredStateTime.get() + System.nanoTime() - measuredStateStart
                : prevMeasuredStateTime.get();
    }

    @Override
    public V apply(InvocationContext<V> ctx) throws Exception {
        LOG.trace("MetricsCollector started");
        try {
            return doApply(ctx);
        } finally {
            LOG.trace("MetricsCollector finished");
        }
    }

    private V doApply(InvocationContext<V> ctx) throws Exception {
        registerMetrics(ctx);

        try {
            V result = delegate.apply(ctx);
            ctx.fireEvent(GeneralMetricsEvents.ExecutionFinished.VALUE_RETURNED);
            return result;
        } catch (Exception e) {
            ctx.fireEvent(GeneralMetricsEvents.ExecutionFinished.EXCEPTION_THROWN);
            throw e;
        }
    }

    protected final void registerMetrics(InvocationContext<V> ctx) {
        // general + fallback

        AtomicBoolean fallbackDefined = new AtomicBoolean(false);
        AtomicBoolean fallbackApplied = new AtomicBoolean(false);
        ctx.registerEventHandler(FallbackEvents.Defined.class, ignored -> fallbackDefined.set(true));
        ctx.registerEventHandler(FallbackEvents.Applied.class, ignored -> fallbackApplied.set(true));

        ctx.registerEventHandler(GeneralMetricsEvents.ExecutionFinished.class,
                event -> metrics.executionFinished(event.succeeded, fallbackDefined.get(), fallbackApplied.get()));

        // retry

        if (hasRetry) {
            AtomicBoolean retried = new AtomicBoolean(false);
            ctx.registerEventHandler(RetryEvents.Retried.class, ignored -> {
                metrics.retryAttempted();
                retried.set(true);
            });
            ctx.registerEventHandler(RetryEvents.Finished.class, event -> {
                if (RetryEvents.Result.VALUE_RETURNED == event.result) {
                    metrics.retryValueReturned(retried.get());
                } else if (RetryEvents.Result.EXCEPTION_NOT_RETRYABLE == event.result) {
                    metrics.retryExceptionNotRetryable(retried.get());
                } else if (RetryEvents.Result.MAX_RETRIES_REACHED == event.result) {
                    metrics.retryMaxRetriesReached(retried.get());
                } else if (RetryEvents.Result.MAX_DURATION_REACHED == event.result) {
                    metrics.retryMaxDurationReached(retried.get());
                }
            });
        }

        // timeout

        if (hasTimeout) {
            AtomicLong timeoutStart = new AtomicLong();
            ctx.registerEventHandler(TimeoutEvents.Started.class,
                    ignored -> timeoutStart.set(System.nanoTime()));
            ctx.registerEventHandler(TimeoutEvents.Finished.class,
                    event -> metrics.timeoutFinished(event.timedOut, System.nanoTime() - timeoutStart.get()));
        }

        // circuit breaker

        if (hasCircuitBreaker) {
            ctx.registerEventHandler(CircuitBreakerEvents.Finished.class,
                    event -> metrics.circuitBreakerFinished(event.result));
            ctx.registerEventHandler(CircuitBreakerEvents.StateTransition.class, event -> {
                state = event.targetState;

                long now = System.nanoTime();

                switch (event.targetState) {
                    case CLOSED:
                        closedStart = now;
                        previousHalfOpenTime.addAndGet(now - halfOpenStart);

                        break;
                    case OPEN:
                        openStart = now;
                        previousClosedTime.addAndGet(now - closedStart);

                        metrics.circuitBreakerMovedToOpen();

                        break;
                    case HALF_OPEN:
                        halfOpenStart = now;
                        previousOpenTime.addAndGet(now - openStart);

                        break;
                }
            });
        }

        // bulkhead

        if (hasBulkhead) {
            AtomicLong runningStart = new AtomicLong();
            ctx.registerEventHandler(BulkheadEvents.DecisionMade.class, event -> metrics.bulkheadDecisionMade(event.accepted));
            ctx.registerEventHandler(BulkheadEvents.StartedRunning.class, ignored -> {
                runningExecutions.incrementAndGet();
                runningStart.set(System.nanoTime());
            });
            ctx.registerEventHandler(BulkheadEvents.FinishedRunning.class, ignored -> {
                runningExecutions.decrementAndGet();
                metrics.updateBulkheadRunningDuration(System.nanoTime() - runningStart.get());
            });

            if (isAsync) {
                AtomicLong waitingStart = new AtomicLong();
                ctx.registerEventHandler(BulkheadEvents.StartedWaiting.class, ignored -> {
                    waitingExecutions.incrementAndGet();
                    waitingStart.set(System.nanoTime());
                });
                ctx.registerEventHandler(BulkheadEvents.FinishedWaiting.class, ignored -> {
                    waitingExecutions.decrementAndGet();
                    metrics.updateBulkheadWaitingDuration(System.nanoTime() - waitingStart.get());
                });
            }
        }

        // rate limit

        if (hasRateLimit) {
            ctx.registerEventHandler(RateLimitEvents.DecisionMade.class,
                    event -> metrics.rateLimitDecisionMade(event.permitted));
        }
    }
}
