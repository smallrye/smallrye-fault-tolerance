package io.smallrye.faulttolerance.core.before.retry;

import static io.smallrye.faulttolerance.core.before.retry.BeforeRetryLogger.LOG;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;

public class BeforeRetry<V> implements FaultToleranceStrategy<V> {

    private final BeforeRetryFunction<V> beforeRetry;

    private final FaultToleranceStrategy<V> delegate;

    public BeforeRetry(FaultToleranceStrategy<V> delegate, BeforeRetryFunction<V> beforeRetry) {
        this.delegate = delegate;
        this.beforeRetry = beforeRetry;
    }

    @Override
    public V apply(InvocationContext<V> ctx) throws Exception {
        LOG.trace("BeforeRetry started");
        try {
            return doApply(ctx);
        } finally {
            LOG.trace("BeforeRetry finished");
        }
    }

    private V doApply(InvocationContext<V> ctx) throws Exception {
        beforeRetry.call(ctx);
        return delegate.apply(ctx);
    }
}