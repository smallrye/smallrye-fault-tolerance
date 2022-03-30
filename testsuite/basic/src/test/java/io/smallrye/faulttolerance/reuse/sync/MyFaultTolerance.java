package io.smallrye.faulttolerance.reuse.sync;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.FaultTolerance;

@ApplicationScoped
public class MyFaultTolerance {
    @Produces
    @Identifier("my-fault-tolerance")
    public static final FaultTolerance<String> FT = FaultTolerance.<String> create()
            .withRetry().maxRetries(2).done()
            .withFallback().handler(() -> "fallback").done()
            .build();
}
