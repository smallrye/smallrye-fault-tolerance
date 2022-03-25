package io.smallrye.faulttolerance.rxjava3.impl;

import java.util.concurrent.CompletionStage;

import io.reactivex.rxjava3.core.Completable;
import io.smallrye.faulttolerance.core.invocation.AsyncSupport;
import io.smallrye.faulttolerance.core.invocation.Invoker;

public class CompletableSupport<T> implements AsyncSupport<T, Completable> {
    @Override
    public String description() {
        return "return " + Completable.class.getSimpleName();
    }

    @Override
    public boolean applies(Class<?>[] parameterTypes, Class<?> returnType) {
        return Completable.class.equals(returnType);
    }

    @Override
    public CompletionStage<T> toCompletionStage(Invoker<Completable> invoker) throws Exception {
        return invoker.proceed().toCompletionStage(null);
    }

    @Override
    public Completable fromCompletionStage(Invoker<CompletionStage<T>> invoker) {
        return Completable.defer(() -> Completable.fromCompletionStage(invoker.proceed()));
    }

    @Override
    public CompletionStage<T> fallbackResultToCompletionStage(Completable completable) {
        return completable.toCompletionStage(null);
    }
}
