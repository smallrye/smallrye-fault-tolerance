package io.smallrye.faulttolerance.reuse.mixed.async.uni;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.mutiny.api.MutinyFaultTolerance;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class MyFaultTolerance {
    // can't define fallback, that's intrinsically typed
    @Produces
    @Identifier("my-fault-tolerance")
    public static final FaultTolerance<Uni<Object>> FT = MutinyFaultTolerance.create()
            .withRetry().maxRetries(5).done()
            .build();
}
