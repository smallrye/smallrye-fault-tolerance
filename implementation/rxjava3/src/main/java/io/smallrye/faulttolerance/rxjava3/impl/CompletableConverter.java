package io.smallrye.faulttolerance.rxjava3.impl;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import io.reactivex.rxjava3.core.Completable;
import io.smallrye.faulttolerance.core.async.types.AsyncTypeConverter;

public class CompletableConverter<T> implements AsyncTypeConverter<T, Completable> {
    @Override
    public Class<?> type() {
        return Completable.class;
    }

    @Override
    public Completable fromCompletionStage(Supplier<CompletionStage<T>> completionStageSupplier) {
        return Completable.defer(() -> Completable.fromCompletionStage(completionStageSupplier.get()));
    }

    @Override
    public CompletionStage<T> toCompletionStage(Completable completable) {
        return completable.toCompletionStage(null);
    }
}
