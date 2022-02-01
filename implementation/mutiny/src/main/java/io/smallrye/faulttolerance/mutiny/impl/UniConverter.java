package io.smallrye.faulttolerance.mutiny.impl;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import io.smallrye.faulttolerance.core.async.types.AsyncTypeConverter;
import io.smallrye.mutiny.Uni;

public class UniConverter<T> implements AsyncTypeConverter<T, Uni<T>> {
    @Override
    public Class<?> type() {
        return Uni.class;
    }

    @Override
    public Uni<T> fromCompletionStage(Supplier<CompletionStage<T>> completionStageSupplier) {
        return Uni.createFrom().completionStage(completionStageSupplier);
    }

    @Override
    public CompletionStage<T> toCompletionStage(Uni<T> uni) {
        return uni.subscribeAsCompletionStage();
    }
}
