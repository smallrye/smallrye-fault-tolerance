package io.smallrye.faulttolerance.reuse.async.completionstage;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.Types;
import io.smallrye.faulttolerance.api.TypedGuard;

@ApplicationScoped
public class MyFaultTolerance {
    @Produces
    @Identifier("my-fault-tolerance")
    public static final TypedGuard<CompletionStage<String>> GUARD = TypedGuard.create(Types.CS_STRING)
            .withRetry().maxRetries(2).done()
            .withFallback().handler(() -> completedFuture("fallback")).done()
            .build();
}
