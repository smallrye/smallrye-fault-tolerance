package io.smallrye.faulttolerance.reuse.async.completionstage.threadoffload.guard;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.Guard;

@Singleton
public class MyFaultTolerance {
    // not `static` to create a new instance for each Weld container in the test
    @Produces
    @Identifier("my-fault-tolerance")
    public final Guard GUARD = Guard.create()
            .withThreadOffload(true)
            .build();
}
