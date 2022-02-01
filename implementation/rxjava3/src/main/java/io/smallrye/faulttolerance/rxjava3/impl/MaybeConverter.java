package io.smallrye.faulttolerance.rxjava3.impl;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import io.reactivex.rxjava3.core.Maybe;
import io.smallrye.faulttolerance.core.async.types.AsyncTypeConverter;

public class MaybeConverter<T> implements AsyncTypeConverter<T, Maybe<T>> {
    @Override
    public Class<?> type() {
        return Maybe.class;
    }

    @Override
    public Maybe<T> fromCompletionStage(Supplier<CompletionStage<T>> completionStageSupplier) {
        return Maybe.defer(() -> Maybe.fromCompletionStage(completionStageSupplier.get()));
    }

    @Override
    public CompletionStage<T> toCompletionStage(Maybe<T> maybe) {
        return maybe.toCompletionStage(null);
    }
}
