package io.smallrye.faulttolerance.reuse.config.typedguard.invalid;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.TypedGuard;

@ApplicationScoped
public class MyFaultTolerance {
    @Produces
    @Identifier("my-fault-tolerance")
    public static final TypedGuard<String> GUARD = TypedGuard.create(String.class)
            .withRetry().maxRetries(2).done()
            .withFallback().handler(() -> "fallback").done()
            .build();
}
