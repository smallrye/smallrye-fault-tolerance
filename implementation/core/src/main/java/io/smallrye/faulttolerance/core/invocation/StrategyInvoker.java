package io.smallrye.faulttolerance.core.invocation;

import java.util.function.Function;

import io.smallrye.faulttolerance.core.FaultToleranceContext;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.Future;

public class StrategyInvoker<V> implements Invoker<Future<V>> {
    private final Object[] arguments; // read-only!
    private final FaultToleranceStrategy<V> strategy;
    private final FaultToleranceContext<V> context;

    public StrategyInvoker(Object[] arguments, FaultToleranceStrategy<V> strategy, FaultToleranceContext<V> context) {
        this.arguments = arguments;
        this.strategy = strategy;
        this.context = context;
    }

    @Override
    public int parametersCount() {
        if (arguments == null) {
            throw new UnsupportedOperationException();
        }
        return arguments.length;
    }

    @Override
    public <T> T getArgument(int index, Class<T> parameterType) {
        if (arguments == null) {
            throw new UnsupportedOperationException();
        }
        return parameterType.cast(arguments[index]);
    }

    @Override
    public <T> T replaceArgument(int index, Class<T> parameterType, Function<T, T> transformation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<V> proceed() throws Exception {
        return strategy.apply(context);
    }
}
