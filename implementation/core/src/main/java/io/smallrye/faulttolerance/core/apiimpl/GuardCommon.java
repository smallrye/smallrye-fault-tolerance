package io.smallrye.faulttolerance.core.apiimpl;

import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import io.smallrye.faulttolerance.core.FaultToleranceContext;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.invocation.AsyncSupport;
import io.smallrye.faulttolerance.core.invocation.AsyncSupportRegistry;
import io.smallrye.faulttolerance.core.invocation.Invoker;
import io.smallrye.faulttolerance.core.invocation.StrategyInvoker;

final class GuardCommon {
    private static final Class<?>[] NO_PARAMS = new Class<?>[0];

    // V = value type, e.g. String
    // T = result type, e.g. String or CompletionStage<String> or Uni<String>
    //
    // in synchronous scenario, V = T
    // in asynchronous scenario, T is an async type that eventually produces V
    static <V, T> AsyncSupport<V, T> asyncSupport(Type type) {
        if (type instanceof Class<?>) {
            return AsyncSupportRegistry.get(NO_PARAMS, (Class<?>) type);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            return AsyncSupportRegistry.get(NO_PARAMS, rawType);
        } else {
            return null;
        }
    }

    // V = value type, e.g. String
    // T = result type, e.g. String or CompletionStage<String> or Uni<String>
    //
    // in synchronous scenario, V = T
    // in asynchronous scenario, T is an async type that eventually produces V
    static <V, T> T guard(Callable<T> action, FaultToleranceStrategy<V> strategy, AsyncSupport<V, T> asyncSupport,
            EventHandlers eventHandlers, Consumer<FaultToleranceContext<?>> contextModifier) throws Exception {
        if (asyncSupport == null) {
            FaultToleranceContext<T> ctx = new FaultToleranceContext<>(() -> Future.from(action), false);
            if (contextModifier != null) {
                contextModifier.accept(ctx);
            }
            eventHandlers.register(ctx);
            try {
                FaultToleranceStrategy<T> castStrategy = (FaultToleranceStrategy<T>) strategy;
                return castStrategy.apply(ctx).awaitBlocking();
            } catch (Exception e) {
                throw e;
            } catch (Throwable e) {
                throw sneakyThrow(e);
            }
        }

        Invoker<T> invoker = new CallableInvoker<>(action);
        FaultToleranceContext<V> ctx = new FaultToleranceContext<>(() -> asyncSupport.toFuture(invoker), true);
        ctx.set(AsyncSupport.class, asyncSupport);
        if (contextModifier != null) {
            contextModifier.accept(ctx);
        }
        eventHandlers.register(ctx);
        Invoker<Future<V>> wrapper = new StrategyInvoker<>(null, strategy, ctx);
        return asyncSupport.fromFuture(wrapper);
    }
}
