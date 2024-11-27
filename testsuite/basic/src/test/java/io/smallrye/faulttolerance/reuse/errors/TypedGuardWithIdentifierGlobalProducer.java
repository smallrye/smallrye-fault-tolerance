package io.smallrye.faulttolerance.reuse.errors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.TypedGuard;

@ApplicationScoped
public class TypedGuardWithIdentifierGlobalProducer {
    @Produces
    @Identifier("global")
    public static TypedGuard<String> GUARD = TypedGuard.create(String.class).build();
}
