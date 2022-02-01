package io.smallrye.faulttolerance.core.async.types;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public interface AsyncTypeConverter<V, AT> {
    Class<?> type();

    AT fromCompletionStage(Supplier<CompletionStage<V>> completionStageSupplier);

    CompletionStage<V> toCompletionStage(AT asyncValue);
}
