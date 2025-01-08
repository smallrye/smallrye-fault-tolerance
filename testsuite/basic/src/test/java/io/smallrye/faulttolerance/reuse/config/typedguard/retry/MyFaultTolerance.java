package io.smallrye.faulttolerance.reuse.config.typedguard.retry;

import java.time.temporal.ChronoUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.TypedGuard;

@ApplicationScoped
public class MyFaultTolerance {
    @Produces
    @Identifier("my-fault-tolerance")
    public static final TypedGuard<String> GUARD = TypedGuard.create(String.class)
            .withRetry().maxRetries(2).delay(10, ChronoUnit.MILLIS).jitter(0, ChronoUnit.MILLIS)
            .retryOn(IllegalArgumentException.class).done()
            .withFallback().handler(() -> "fallback").done()
            .build();
}
