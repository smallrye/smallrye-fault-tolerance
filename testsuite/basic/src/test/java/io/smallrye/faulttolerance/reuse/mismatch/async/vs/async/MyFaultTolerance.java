package io.smallrye.faulttolerance.reuse.mismatch.async.vs.async;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.mutiny.api.MutinyFaultTolerance;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class MyFaultTolerance {
    @Produces
    @Identifier("my-fault-tolerance")
    public static final FaultTolerance<Uni<String>> FT = MutinyFaultTolerance.<String> create().build();
}
