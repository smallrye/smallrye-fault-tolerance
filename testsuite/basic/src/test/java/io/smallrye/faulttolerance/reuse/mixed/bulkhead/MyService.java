package io.smallrye.faulttolerance.reuse.mixed.bulkhead;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;

import io.smallrye.faulttolerance.api.ApplyGuard;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class MyService {
    @ApplyGuard("my-fault-tolerance")
    public String hello(Barrier barrier) throws InterruptedException {
        barrier.await();
        return "hello";
    }

    @ApplyGuard("my-fault-tolerance")
    @Asynchronous
    public CompletionStage<Integer> theAnswer(Barrier barrier) throws InterruptedException {
        barrier.await();
        return completedFuture(42);
    }

    @ApplyGuard("my-fault-tolerance")
    @Asynchronous
    public Uni<Long> badNumber(Barrier barrier) throws InterruptedException {
        barrier.await();
        return Uni.createFrom().item(13L);
    }
}
