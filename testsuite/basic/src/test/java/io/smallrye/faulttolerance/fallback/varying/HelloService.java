package io.smallrye.faulttolerance.fallback.varying;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Fallback;

@ApplicationScoped
public class HelloService {
    @Fallback(fallbackMethod = "fallback")
    public String hello(int counter) throws IOException {
        throw new IOException();
    }

    public String fallback(int counter) {
        return "Hello " + counter;
    }

    // ---

    @Asynchronous
    @Fallback(fallbackMethod = "fallbackCompletionStage")
    public CompletionStage<String> helloCompletionStageFailingSync(int counter) throws IOException {
        throw new IOException();
    }

    @Asynchronous
    @Fallback(fallbackMethod = "fallbackCompletionStage")
    public CompletionStage<String> helloCompletionStageFailingAsync(int counter) throws IOException {
        CompletableFuture<String> result = new CompletableFuture<>();
        result.completeExceptionally(new IOException());
        return result;
    }

    public CompletionStage<String> fallbackCompletionStage(int counter) {
        return CompletableFuture.completedFuture("Hello " + counter);
    }

    // ---

    @Asynchronous
    @Fallback(fallbackMethod = "fallbackFuture")
    public Future<String> helloFutureFailingSync(int counter) throws IOException {
        throw new IOException();
    }

    @Asynchronous
    @Fallback(fallbackMethod = "fallbackFuture")
    public Future<String> helloFutureFailingAsync(int counter) throws IOException {
        CompletableFuture<String> result = new CompletableFuture<>();
        result.completeExceptionally(new IOException());
        return result;
    }

    public Future<String> fallbackFuture(int counter) {
        return CompletableFuture.completedFuture("Hello " + counter);
    }
}
