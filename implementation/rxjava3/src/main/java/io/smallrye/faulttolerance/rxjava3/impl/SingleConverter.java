package io.smallrye.faulttolerance.rxjava3.impl;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import io.reactivex.rxjava3.core.Single;
import io.smallrye.faulttolerance.core.async.types.AsyncTypeConverter;

public class SingleConverter<T> implements AsyncTypeConverter<T, Single<T>> {
    @Override
    public Class<?> type() {
        return Single.class;
    }

    @Override
    public Single<T> fromCompletionStage(Supplier<CompletionStage<T>> completionStageSupplier) {
        return Single.defer(() -> Single.fromCompletionStage(completionStageSupplier.get()));
    }

    @Override
    public CompletionStage<T> toCompletionStage(Single<T> single) {
        return single.toCompletionStage();
    }
}
