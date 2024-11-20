package io.smallrye.faulttolerance.reuse.async.completionstage.fallback.guard;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.Guard;

@ApplicationScoped
public class MyFaultTolerance {
    @Produces
    @Identifier("my-fault-tolerance")
    public static final Guard GUARD = Guard.create()
            .withRetry().maxRetries(2).done()
            .build();
}
