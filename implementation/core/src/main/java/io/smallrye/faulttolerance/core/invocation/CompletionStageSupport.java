package io.smallrye.faulttolerance.core.invocation;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import io.smallrye.faulttolerance.core.Completer;
import io.smallrye.faulttolerance.core.Future;

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
    public CompletionStage<T> createComplete(T value) {
        return completedFuture(value);
    }

    @Override
    public Future<T> toFuture(Invoker<CompletionStage<T>> invoker) {
        Completer<T> completer = Completer.create();
        try {
            invoker.proceed().whenComplete((value, error) -> {
                if (error == null) {
                    completer.complete(value);
                } else {
                    if (error instanceof CompletionException) {
                        completer.completeWithError(error.getCause());
                    } else {
                        completer.completeWithError(error);
                    }
                }
            });
        } catch (Exception e) {
            completer.completeWithError(e);
        }
        return completer.future();
    }

    @Override
    public CompletionStage<T> fromFuture(Invoker<Future<T>> invoker) {
        CompletableFuture<T> result = new CompletableFuture<>();
        try {
            invoker.proceed().then((value, error) -> {
                if (error == null) {
                    result.complete(value);
                } else {
                    result.completeExceptionally(error);
                }
            });
        } catch (Exception e) {
            result.completeExceptionally(e);
        }
        return result;
    }
}
