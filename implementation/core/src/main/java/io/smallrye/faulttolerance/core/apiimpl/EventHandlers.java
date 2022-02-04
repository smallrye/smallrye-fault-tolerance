package io.smallrye.faulttolerance.core.apiimpl;

import java.util.function.Consumer;

import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.bulkhead.BulkheadEvents;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerEvents;
import io.smallrye.faulttolerance.core.retry.RetryEvents;
import io.smallrye.faulttolerance.core.timeout.TimeoutEvents;

final class EventHandlers {
    private final Runnable bulkheadOnAccepted;
    private final Runnable bulkheadOnRejected;
    private final Runnable bulkheadOnFinished;

    private final Consumer<CircuitBreakerState> circuitBreakerOnStateChange;
    private final Runnable circuitBreakerOnSuccess;
    private final Runnable circuitBreakerOnFailure;
    private final Runnable circuitBreakerOnPrevented;

    private final Runnable retryOnRetry;
    private final Runnable retryOnSuccess;
    private final Runnable retryOnFailure;

    private final Runnable timeoutOnTimeout;
    private final Runnable timeoutOnFinished;

    EventHandlers(Runnable bulkheadOnAccepted, Runnable bulkheadOnRejected, Runnable bulkheadOnFinished,
            Consumer<CircuitBreakerState> circuitBreakerOnStateChange, Runnable circuitBreakerOnSuccess,
            Runnable circuitBreakerOnFailure, Runnable circuitBreakerOnPrevented, Runnable retryOnRetry,
            Runnable retryOnSuccess, Runnable retryOnFailure, Runnable timeoutOnTimeout, Runnable timeoutOnFinished) {
        this.bulkheadOnAccepted = wrap(bulkheadOnAccepted);
        this.bulkheadOnRejected = wrap(bulkheadOnRejected);
        this.bulkheadOnFinished = wrap(bulkheadOnFinished);
        this.circuitBreakerOnStateChange = wrap(circuitBreakerOnStateChange);
        this.circuitBreakerOnSuccess = wrap(circuitBreakerOnSuccess);
        this.circuitBreakerOnFailure = wrap(circuitBreakerOnFailure);
        this.circuitBreakerOnPrevented = wrap(circuitBreakerOnPrevented);
        this.retryOnRetry = wrap(retryOnRetry);
        this.retryOnSuccess = wrap(retryOnSuccess);
        this.retryOnFailure = wrap(retryOnFailure);
        this.timeoutOnTimeout = wrap(timeoutOnTimeout);
        this.timeoutOnFinished = wrap(timeoutOnFinished);
    }

    private static <T> Consumer<T> wrap(Consumer<T> callback) {
        if (callback == null) {
            return null;
        }

        return value -> {
            try {
                callback.accept(value);
            } catch (Exception ignored) {
            }
        };
    }

    private static Runnable wrap(Runnable callback) {
        if (callback == null) {
            return null;
        }

        return () -> {
            try {
                callback.run();
            } catch (Exception ignored) {
            }
        };
    }

    void register(InvocationContext<?> ctx) {
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
