package io.smallrye.faulttolerance.reuse.async.uni.threadoffload.typedguard;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.Types;
import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.mutiny.Uni;

@Singleton
public class MyFaultTolerance {
    // not `static` to create a new instance for each Weld container in the test
    @Produces
    @Identifier("my-fault-tolerance")
    public final TypedGuard<Uni<String>> GUARD = TypedGuard.create(Types.UNI_STRING)
            .withThreadOffload(true)
            .build();
}
