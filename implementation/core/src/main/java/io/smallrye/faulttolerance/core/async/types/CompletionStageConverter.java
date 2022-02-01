package io.smallrye.faulttolerance.core.async.types;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public class CompletionStageConverter<T> implements AsyncTypeConverter<T, CompletionStage<T>> {
    @Override
    public Class<?> type() {
        return CompletionStage.class;
    }

    @Override
    public CompletionStage<T> fromCompletionStage(Supplier<CompletionStage<T>> completionStageSupplier) {
        return completionStageSupplier.get();
    }

    @Override
    public CompletionStage<T> toCompletionStage(CompletionStage<T> asyncValue) {
        return asyncValue;
    }
}
