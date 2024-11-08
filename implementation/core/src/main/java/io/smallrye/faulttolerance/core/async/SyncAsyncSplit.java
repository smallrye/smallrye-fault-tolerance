package io.smallrye.faulttolerance.core.async;

import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import io.smallrye.faulttolerance.core.FaultToleranceContext;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.Future;

public class SyncAsyncSplit<V> implements FaultToleranceStrategy<V> {
    private final FaultToleranceStrategy<V> asyncDelegate;
    private final FaultToleranceStrategy<V> syncDelegate;

    public SyncAsyncSplit(FaultToleranceStrategy<V> asyncDelegate, FaultToleranceStrategy<V> syncDelegate) {
        this.asyncDelegate = checkNotNull(asyncDelegate, "Async delegate must be set");
        this.syncDelegate = checkNotNull(syncDelegate, "Sync delegate must be set");
    }

    @Override
    public Future<V> apply(FaultToleranceContext<V> ctx) {
        if (ctx.isAsync()) {
            return asyncDelegate.apply(ctx);
        } else {
            return syncDelegate.apply(ctx);
        }
    }
}
