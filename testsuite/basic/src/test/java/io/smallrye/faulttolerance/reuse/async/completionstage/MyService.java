package io.smallrye.faulttolerance.reuse.async.completionstage;

import static io.smallrye.faulttolerance.core.util.CompletionStages.failedFuture;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.ApplyFaultTolerance;

@ApplicationScoped
public class MyService {
    static final AtomicInteger COUNTER = new AtomicInteger(0);

    @ApplyFaultTolerance("my-fault-tolerance")
    public CompletionStage<String> hello() {
        COUNTER.incrementAndGet();
        return failedFuture(new IllegalArgumentException());
    }
}
