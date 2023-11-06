package io.smallrye.faulttolerance.core.invocation;

import static java.util.concurrent.CompletableFuture.failedFuture;

import java.util.concurrent.CompletionStage;

public class CompletionStageSupport<T> implements AsyncSupport<T, CompletionStage<T>> {
    @Override
    public String mustDescription() {
        return "return " + CompletionStage.class.getSimpleName();
    }

    @Override
    public String doesDescription() {
        return "returns " + CompletionStage.class.getSimpleName();
    }

    @Override
    public boolean applies(Class<?>[] parameterTypes, Class<?> returnType) {
        return CompletionStage.class.equals(returnType);
    }

    @Override
    public CompletionStage<T> toCompletionStage(Invoker<CompletionStage<T>> invoker) throws Exception {
        return invoker.proceed();
    }

    @Override
    public CompletionStage<T> fromCompletionStage(Invoker<CompletionStage<T>> invoker) {
        try {
            return invoker.proceed();
        } catch (Exception e) {
            return failedFuture(e);
        }
    }

    // ---

    @Override
    public CompletionStage<T> fallbackResultToCompletionStage(CompletionStage<T> completionStage) {
        return completionStage;
    }
}
