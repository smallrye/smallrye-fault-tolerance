package io.smallrye.faulttolerance.reuse.mismatch.sync.vs.async;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.FaultTolerance;

@ApplicationScoped
public class MyFaultTolerance {
    @Produces
    @Identifier("my-fault-tolerance")
    public static final FaultTolerance<String> FT = FaultTolerance.<String> create().build();
}
