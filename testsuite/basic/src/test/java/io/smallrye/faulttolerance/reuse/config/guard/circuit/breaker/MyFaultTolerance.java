package io.smallrye.faulttolerance.reuse.config.guard.circuit.breaker;

import java.time.temporal.ChronoUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.Guard;

@ApplicationScoped
public class MyFaultTolerance {
    @Produces
    @Identifier("my-fault-tolerance")
    public static final Guard GUARD = Guard.create()
            .withCircuitBreaker().requestVolumeThreshold(10).delay(20, ChronoUnit.SECONDS).failOn(IllegalStateException.class)
            .skipOn(IllegalArgumentException.class).done()
            .build();
}
