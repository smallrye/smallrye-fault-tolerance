package io.smallrye.faulttolerance.internal;

import javax.enterprise.event.Event;

import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.faulttolerance.api.CircuitBreakerStateChanged;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerEvents;

public class CircuitBreakerStateObserver<V> implements FaultToleranceStrategy<V> {
    private final FaultToleranceStrategy<V> delegate;
    private final InterceptionPoint interceptionPoint;
    private final Event<CircuitBreakerStateChanged> cbStateEvent;

    public CircuitBreakerStateObserver(FaultToleranceStrategy<V> delegate, InterceptionPoint interceptionPoint,
            Event<CircuitBreakerStateChanged> cbStateEvent) {
        this.delegate = delegate;
        this.interceptionPoint = interceptionPoint;
        this.cbStateEvent = cbStateEvent;
    }

    @Override
    public V apply(InvocationContext<V> ctx) throws Exception {
        ctx.registerEventHandler(CircuitBreakerEvents.StateTransition.class, event -> {
            cbStateEvent.fire(new CircuitBreakerStateChanged(
                    interceptionPoint.beanClass(),
                    interceptionPoint.method(),
                    CircuitBreakerState.valueOf(event.targetState.name())));
        });

        return delegate.apply(ctx);
    }
}
