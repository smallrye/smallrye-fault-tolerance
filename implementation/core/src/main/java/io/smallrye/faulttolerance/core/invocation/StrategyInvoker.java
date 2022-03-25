package io.smallrye.faulttolerance.core.invocation;

import java.util.function.Function;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;

public class StrategyInvoker<V> implements Invoker<V> {
    private final Object[] arguments; // read-only!
    private final FaultToleranceStrategy<V> strategy;
    private final InvocationContext<V> context;

    public StrategyInvoker(Object[] arguments, FaultToleranceStrategy<V> strategy, InvocationContext<V> context) {
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
    public V proceed() throws Exception {
        return strategy.apply(context);
    }
}
