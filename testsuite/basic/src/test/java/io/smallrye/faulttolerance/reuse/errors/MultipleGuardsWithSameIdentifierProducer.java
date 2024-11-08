package io.smallrye.faulttolerance.reuse.errors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.Guard;
import io.smallrye.faulttolerance.api.TypedGuard;

@ApplicationScoped
public class MultipleGuardsWithSameIdentifierProducer {
    @Produces
    @Identifier("foobar")
    public static Guard GUARD = Guard.create().build();

    @Produces
    @Identifier("foobar")
    public static TypedGuard<String> TYPED_GUARD = TypedGuard.create(String.class).build();
}
