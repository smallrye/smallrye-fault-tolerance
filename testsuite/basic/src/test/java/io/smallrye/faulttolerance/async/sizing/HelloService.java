package io.smallrye.faulttolerance.async.sizing;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;

@ApplicationScoped
public class HelloService {

    @Asynchronous
    public CompletionStage<String> hello(CountDownLatch startLatch, CountDownLatch endLatch) throws InterruptedException {
        startLatch.countDown();
        endLatch.await();
        return completedFuture("hello");
    }
}
