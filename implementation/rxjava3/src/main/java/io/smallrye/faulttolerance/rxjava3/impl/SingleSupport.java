package io.smallrye.faulttolerance.rxjava3.impl;

import java.util.concurrent.CompletionStage;

import io.reactivex.rxjava3.core.Single;
import io.smallrye.faulttolerance.core.invocation.AsyncSupport;
import io.smallrye.faulttolerance.core.invocation.Invoker;

public class SingleSupport<T> implements AsyncSupport<T, Single<T>> {
    @Override
    public String mustDescription() {
        return "return " + Single.class.getSimpleName();
    }

    @Override
    public String doesDescription() {
        return "returns " + Single.class.getSimpleName();
    }

    @Override
    public boolean applies(Class<?>[] parameterTypes, Class<?> returnType) {
        return Single.class.equals(returnType);
    }

    @Override
    public Single<T> createComplete(T value) {
        return Single.just(value);
    }

    @Override
    public CompletionStage<T> toCompletionStage(Invoker<Single<T>> invoker) throws Exception {
        return invoker.proceed().toCompletionStage();
    }

    @Override
    public Single<T> fromCompletionStage(Invoker<CompletionStage<T>> invoker) {
        return Single.defer(() -> Single.fromCompletionStage(invoker.proceed()));
    }

    @Override
    public CompletionStage<T> fallbackResultToCompletionStage(Single<T> single) {
        return single.toCompletionStage();
    }
}
