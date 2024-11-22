package io.smallrye.faulttolerance.core.apiimpl;

import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import io.smallrye.faulttolerance.core.invocation.AsyncSupport;
import io.smallrye.faulttolerance.core.invocation.Invoker;

public final class AsyncInvocation<V, AT> {
    public final AsyncSupport<V, AT> asyncSupport;
    public final Invoker<AT> toFutureInvoker;
    public final Object[] arguments; // to create a `StrategyInvoker`, which is always used as a `fromFutureInvoker`

    public AsyncInvocation(AsyncSupport<V, AT> asyncSupport, Invoker<AT> toFutureInvoker, Object[] arguments) {
        this.asyncSupport = checkNotNull(asyncSupport, "asyncSupport must be set");
        this.toFutureInvoker = checkNotNull(toFutureInvoker, "toFutureInvoker must be set");
        this.arguments = arguments;
    }
}
