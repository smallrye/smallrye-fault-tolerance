package io.smallrye.faulttolerance.reuse.errors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.Guard;

@ApplicationScoped
public class GuardWithIdentifierGlobalProducer {
    @Produces
    @Identifier("global")
    public static Guard GUARD = Guard.create().build();
}
