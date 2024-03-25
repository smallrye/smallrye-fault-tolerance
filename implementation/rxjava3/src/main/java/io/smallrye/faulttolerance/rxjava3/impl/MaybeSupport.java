package io.smallrye.faulttolerance.rxjava3.impl;

import java.util.concurrent.CompletionStage;

import io.reactivex.rxjava3.core.Maybe;
import io.smallrye.faulttolerance.core.invocation.AsyncSupport;
import io.smallrye.faulttolerance.core.invocation.Invoker;

public class MaybeSupport<T> implements AsyncSupport<T, Maybe<T>> {
    @Override
    public String mustDescription() {
        return "return " + Maybe.class.getSimpleName();
    }

    @Override
    public String doesDescription() {
        return "returns " + Maybe.class.getSimpleName();
    }

    @Override
    public boolean applies(Class<?>[] parameterTypes, Class<?> returnType) {
        return Maybe.class.equals(returnType);
    }

    @Override
    public Maybe<T> createComplete(T value) {
        return Maybe.just(value);
    }

    @Override
    public CompletionStage<T> toCompletionStage(Invoker<Maybe<T>> invoker) throws Exception {
        return invoker.proceed().toCompletionStage(null);
    }

    @Override
    public Maybe<T> fromCompletionStage(Invoker<CompletionStage<T>> invoker) {
        return Maybe.defer(() -> Maybe.fromCompletionStage(invoker.proceed()));
    }

    @Override
    public CompletionStage<T> fallbackResultToCompletionStage(Maybe<T> maybe) {
        return maybe.toCompletionStage(null);
    }
}
