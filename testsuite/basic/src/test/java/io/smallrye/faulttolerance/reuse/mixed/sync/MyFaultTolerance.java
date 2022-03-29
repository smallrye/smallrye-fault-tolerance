package io.smallrye.faulttolerance.reuse.mixed.sync;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.FaultTolerance;

@ApplicationScoped
public class MyFaultTolerance {
    // can't define fallback, that's intrinsically typed
    @Produces
    @Identifier("my-fault-tolerance")
    public static final FaultTolerance<Object> FT = FaultTolerance.create()
            .withRetry().maxRetries(5).done()
            .build();
}
