package io.smallrye.faulttolerance.reuse.config.guard.ratelimit;

import java.time.temporal.ChronoUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.Guard;
import io.smallrye.faulttolerance.api.RateLimitType;

@ApplicationScoped
public class MyFaultTolerance {
    @Produces
    @Identifier("my-fault-tolerance")
    public static final Guard GUARD = Guard.create()
            .withRateLimit().limit(50).window(20, ChronoUnit.SECONDS).type(RateLimitType.SMOOTH).done()
            .build();
}
