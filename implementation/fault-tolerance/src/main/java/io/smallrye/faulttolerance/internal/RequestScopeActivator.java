package io.smallrye.faulttolerance.internal;

import static io.smallrye.faulttolerance.internal.InternalLogger.LOG;

import jakarta.enterprise.context.control.RequestContextController;

import io.smallrye.faulttolerance.core.FaultToleranceContext;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.Future;

public class RequestScopeActivator<V> implements FaultToleranceStrategy<V> {
    private final FaultToleranceStrategy<V> delegate;
    private final RequestContextController requestContextController;

    public RequestScopeActivator(FaultToleranceStrategy<V> delegate, RequestContextController requestContextController) {
        this.delegate = delegate;
        this.requestContextController = requestContextController;
    }

    @Override
    public Future<V> apply(FaultToleranceContext<V> ctx) {
        // for CompletionStage, the requestContextController.activate/deactivate pair here
        // is the minimum to pass TCK; for anything serious, Context Propagation is required

        LOG.trace("RequestScopeActivator started");
        try {
            requestContextController.activate();
            return delegate.apply(ctx);
        } finally {
            requestContextController.deactivate();
            LOG.trace("RequestScopeActivator finished");
        }
    }
}
