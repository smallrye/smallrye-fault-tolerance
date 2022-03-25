package io.smallrye.faulttolerance.mutiny.impl;

import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;

import java.util.concurrent.CompletionStage;

import io.smallrye.faulttolerance.core.invocation.AsyncSupport;
import io.smallrye.faulttolerance.core.invocation.Invoker;
import io.smallrye.mutiny.Uni;

public class UniSupport<T> implements AsyncSupport<T, Uni<T>> {
    @Override
    public String description() {
        return "return " + Uni.class.getSimpleName();
    }

    @Override
    public boolean applies(Class<?>[] parameterTypes, Class<?> returnType) {
        return Uni.class.equals(returnType);
    }

    @Override
    public CompletionStage<T> toCompletionStage(Invoker<Uni<T>> invoker) throws Exception {
        return invoker.proceed().subscribeAsCompletionStage();
    }

    @Override
    public Uni<T> fromCompletionStage(Invoker<CompletionStage<T>> invoker) {
        return Uni.createFrom().completionStage(() -> {
            try {
                return invoker.proceed();
            } catch (Exception e) {
                throw sneakyThrow(e);
            }
        });
    }

    // ---

    @Override
    public CompletionStage<T> fallbackResultToCompletionStage(Uni<T> uni) {
        return uni.subscribeAsCompletionStage();
    }
}
