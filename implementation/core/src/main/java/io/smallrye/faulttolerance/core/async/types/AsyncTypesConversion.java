package io.smallrye.faulttolerance.core.async.types;

import static io.smallrye.faulttolerance.core.async.types.AsyncTypesLogger.LOG;
import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class AsyncTypesConversion {
    public static class ToCompletionStage implements FaultToleranceStrategy {
        private final FaultToleranceStrategy delegate;
        private final AsyncTypeConverter converter;

        public ToCompletionStage(FaultToleranceStrategy delegate, AsyncTypeConverter converter) {
            this.delegate = delegate;
            this.converter = converter;
        }

        @Override
        public Object apply(InvocationContext ctx) throws Exception {
            LOG.trace("AsyncTypesConversion.ToCompletionStage started");
            try {
                return converter.toCompletionStage(delegate.apply(ctx));
            } finally {
                LOG.trace("AsyncTypesConversion.ToCompletionStage finished");
            }
        }
    }

    public static class FromCompletionStage implements FaultToleranceStrategy {
        private final FaultToleranceStrategy delegate;
        private final AsyncTypeConverter converter;

        public FromCompletionStage(FaultToleranceStrategy delegate, AsyncTypeConverter converter) {
            this.delegate = delegate;
            this.converter = converter;
        }

        @Override
        public Object apply(InvocationContext ctx) throws Exception {
            LOG.trace("AsyncTypesConversion.FromCompletionStage started");
            try {
                return converter.fromCompletionStage(() -> {
                    try {
                        return delegate.apply(ctx);
                    } catch (Exception e) {
                        throw sneakyThrow(e);
                    }
                });
            } finally {
                LOG.trace("AsyncTypesConversion.FromCompletionStage finished");
            }
        }
    }
}
