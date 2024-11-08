package io.smallrye.faulttolerance.reuse.async.uni;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.Types;
import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class MyFaultTolerance {
    @Produces
    @Identifier("my-fault-tolerance")
    public static final TypedGuard<Uni<String>> GUARD = TypedGuard.create(Types.UNI_STRING)
            .withRetry().maxRetries(2).done()
            .withFallback().handler(() -> Uni.createFrom().item("fallback")).done()
            .build();
}
