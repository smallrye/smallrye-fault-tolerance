package io.smallrye.faulttolerance.reuse.async.completionstage;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.FaultTolerance;

@ApplicationScoped
public class MyFaultTolerance {
    @Produces
    @Identifier("my-fault-tolerance")
    public static final FaultTolerance<CompletionStage<String>> FT = FaultTolerance.<String> createAsync()
            .withRetry().maxRetries(2).done()
            .withFallback().handler(() -> completedFuture("fallback")).done()
            .build();
}
