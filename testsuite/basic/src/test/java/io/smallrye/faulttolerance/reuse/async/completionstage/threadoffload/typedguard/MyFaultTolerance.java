package io.smallrye.faulttolerance.reuse.async.completionstage.threadoffload.typedguard;

import java.util.concurrent.CompletionStage;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.Types;
import io.smallrye.faulttolerance.api.TypedGuard;

@Singleton
public class MyFaultTolerance {
    // not `static` to create a new instance for each Weld container in the test
    @Produces
    @Identifier("my-fault-tolerance")
    public final TypedGuard<CompletionStage<String>> GUARD = TypedGuard.create(Types.CS_STRING)
            .withThreadOffload(true)
            .build();
}
