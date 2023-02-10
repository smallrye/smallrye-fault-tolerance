package io.smallrye.faulttolerance.reuse.mixed.async.completionstage;

import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.FaultTolerance;

@ApplicationScoped
public class MyFaultTolerance {
    // can't define fallback, that's intrinsically typed
    @Produces
    @Identifier("my-fault-tolerance")
    public static final FaultTolerance<CompletionStage<Object>> FT = FaultTolerance.createAsync()
            .withRetry().maxRetries(5).done()
            .build();
}
