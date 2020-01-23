package io.smallrye.faulttolerance.internal;

import javax.enterprise.context.control.RequestContextController;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;

public class RequestScopeActivator<V> implements FaultToleranceStrategy<V> {
    private final FaultToleranceStrategy<V> delegate;
    private final RequestContextController requestContextController;

    public RequestScopeActivator(FaultToleranceStrategy<V> delegate, RequestContextController requestContextController) {
        this.delegate = delegate;
        this.requestContextController = requestContextController;
    }

    @Override
    public V apply(InvocationContext<V> ctx) throws Exception {
        try {
            requestContextController.activate();
            return delegate.apply(ctx);
        } finally {
            requestContextController.deactivate();
        }
    }
}
