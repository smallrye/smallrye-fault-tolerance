package io.smallrye.faulttolerance.internal;

import java.util.concurrent.CompletionStage;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.reactive.converters.ReactiveTypeConverter;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class AsyncTypesConversion {
    public static class ToCompletionStage implements FaultToleranceStrategy {
        private final FaultToleranceStrategy delegate;
        private final ReactiveTypeConverter converter;

        public ToCompletionStage(FaultToleranceStrategy delegate, ReactiveTypeConverter converter) {
            this.delegate = delegate;
            this.converter = converter;
        }

        @Override
        public Object apply(InvocationContext ctx) throws Exception {
            Object result = delegate.apply(ctx);
            return converter.toCompletionStage(result);
        }
    }

    public static class FromCompletionStage implements FaultToleranceStrategy {
        private final FaultToleranceStrategy delegate;
        private final ReactiveTypeConverter converter;

        public FromCompletionStage(FaultToleranceStrategy delegate, ReactiveTypeConverter converter) {
            this.delegate = delegate;
            this.converter = converter;
        }

        @Override
        public Object apply(InvocationContext ctx) throws Exception {
            CompletionStage result = (CompletionStage) delegate.apply(ctx);
            return converter.fromCompletionStage(result);
        }
    }
}
