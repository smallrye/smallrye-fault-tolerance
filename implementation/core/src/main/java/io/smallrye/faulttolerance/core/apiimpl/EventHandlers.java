package io.smallrye.faulttolerance.core.apiimpl;

import java.util.function.Consumer;

import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.faulttolerance.core.FaultToleranceContext;
import io.smallrye.faulttolerance.core.bulkhead.BulkheadEvents;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerEvents;
import io.smallrye.faulttolerance.core.rate.limit.RateLimitEvents;
import io.smallrye.faulttolerance.core.retry.RetryEvents;
import io.smallrye.faulttolerance.core.timeout.TimeoutEvents;
import io.smallrye.faulttolerance.core.util.Callbacks;

final class EventHandlers {
    private final Runnable bulkheadOnAccepted;
    private final Runnable bulkheadOnRejected;
    private final Runnable bulkheadOnFinished;

    private final Consumer<CircuitBreakerEvents.StateTransition> cbMaintenanceEventHandler;
    private final Consumer<CircuitBreakerState> circuitBreakerOnStateChange;
    private final Runnable circuitBreakerOnSuccess;
    private final Runnable circuitBreakerOnFailure;
    private final Runnable circuitBreakerOnPrevented;

    private final Runnable rateLimitOnPermitted;
    private final Runnable rateLimitOnRejected;

    private final Runnable retryOnRetry;
    private final Runnable retryOnSuccess;
    private final Runnable retryOnFailure;

    private final Runnable timeoutOnTimeout;
    private final Runnable timeoutOnFinished;

    EventHandlers(Runnable bulkheadOnAccepted, Runnable bulkheadOnRejected, Runnable bulkheadOnFinished,
            Consumer<CircuitBreakerEvents.StateTransition> cbMaintenanceEventHandler,
            Consumer<CircuitBreakerState> circuitBreakerOnStateChange, Runnable circuitBreakerOnSuccess,
            Runnable circuitBreakerOnFailure, Runnable circuitBreakerOnPrevented, Runnable rateLimitOnPermitted,
            Runnable rateLimitOnRejected, Runnable retryOnRetry, Runnable retryOnSuccess, Runnable retryOnFailure,
            Runnable timeoutOnTimeout, Runnable timeoutOnFinished) {
        this.bulkheadOnAccepted = Callbacks.wrap(bulkheadOnAccepted);
        this.bulkheadOnRejected = Callbacks.wrap(bulkheadOnRejected);
        this.bulkheadOnFinished = Callbacks.wrap(bulkheadOnFinished);
        this.cbMaintenanceEventHandler = Callbacks.wrap(cbMaintenanceEventHandler);
        this.circuitBreakerOnStateChange = Callbacks.wrap(circuitBreakerOnStateChange);
        this.circuitBreakerOnSuccess = Callbacks.wrap(circuitBreakerOnSuccess);
        this.circuitBreakerOnFailure = Callbacks.wrap(circuitBreakerOnFailure);
        this.circuitBreakerOnPrevented = Callbacks.wrap(circuitBreakerOnPrevented);
        this.rateLimitOnPermitted = Callbacks.wrap(rateLimitOnPermitted);
        this.rateLimitOnRejected = Callbacks.wrap(rateLimitOnRejected);
        this.retryOnRetry = Callbacks.wrap(retryOnRetry);
        this.retryOnSuccess = Callbacks.wrap(retryOnSuccess);
        this.retryOnFailure = Callbacks.wrap(retryOnFailure);
        this.timeoutOnTimeout = Callbacks.wrap(timeoutOnTimeout);
        this.timeoutOnFinished = Callbacks.wrap(timeoutOnFinished);
    }

    void register(FaultToleranceContext<?> ctx) {
        if (bulkheadOnAccepted != null || bulkheadOnRejected != null) {
            ctx.registerEventHandler(BulkheadEvents.DecisionMade.class, event -> {
                if (event.accepted) {
                    if (bulkheadOnAccepted != null) {
                        bulkheadOnAccepted.run();
                    }
                } else {
                    if (bulkheadOnRejected != null) {
                        bulkheadOnRejected.run();
                    }
                }
            });
        }
        if (bulkheadOnFinished != null) {
            ctx.registerEventHandler(BulkheadEvents.FinishedRunning.class, event -> {
                bulkheadOnFinished.run();
            });
        }

        if (cbMaintenanceEventHandler != null) {
            ctx.registerEventHandler(CircuitBreakerEvents.StateTransition.class, cbMaintenanceEventHandler);
        }
        if (circuitBreakerOnStateChange != null) {
            ctx.registerEventHandler(CircuitBreakerEvents.StateTransition.class, event -> {
                CircuitBreakerState targetState = event.targetState;
                circuitBreakerOnStateChange.accept(targetState);
            });
        }
        if (circuitBreakerOnSuccess != null || circuitBreakerOnFailure != null || circuitBreakerOnPrevented != null) {
            ctx.registerEventHandler(CircuitBreakerEvents.Finished.class, event -> {
                switch (event.result) {
                    case SUCCESS:
                        if (circuitBreakerOnSuccess != null) {
                            circuitBreakerOnSuccess.run();
                        }
                        break;
                    case FAILURE:
                        if (circuitBreakerOnFailure != null) {
                            circuitBreakerOnFailure.run();
                        }
                        break;
                    case PREVENTED:
                        if (circuitBreakerOnPrevented != null) {
                            circuitBreakerOnPrevented.run();
                        }
                        break;
                }
            });
        }

        if (rateLimitOnPermitted != null || rateLimitOnRejected != null) {
            ctx.registerEventHandler(RateLimitEvents.DecisionMade.class, event -> {
                if (event.permitted) {
                    if (rateLimitOnPermitted != null) {
                        rateLimitOnPermitted.run();
                    }
                } else {
                    if (rateLimitOnRejected != null) {
                        rateLimitOnRejected.run();
                    }
                }
            });
        }

        if (retryOnRetry != null) {
            ctx.registerEventHandler(RetryEvents.Retried.class, event -> {
                retryOnRetry.run();
            });
        }
        if (retryOnSuccess != null || retryOnFailure != null) {
            ctx.registerEventHandler(RetryEvents.Finished.class, event -> {
                if (event.result == RetryEvents.Result.VALUE_RETURNED) {
                    if (retryOnSuccess != null) {
                        retryOnSuccess.run();
                    }
                } else {
                    if (retryOnFailure != null) {
                        retryOnFailure.run();
                    }
                }
            });
        }

        if (timeoutOnTimeout != null || timeoutOnFinished != null) {
            ctx.registerEventHandler(TimeoutEvents.Finished.class, event -> {
                if (event.timedOut) {
                    if (timeoutOnTimeout != null) {
                        timeoutOnTimeout.run();
                    }
                } else {
                    if (timeoutOnFinished != null) {
                        timeoutOnFinished.run();
                    }
                }
            });
        }
    }
}
