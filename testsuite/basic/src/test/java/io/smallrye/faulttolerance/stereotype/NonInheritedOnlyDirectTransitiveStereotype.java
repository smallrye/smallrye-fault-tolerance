package io.smallrye.faulttolerance.stereotype;

import jakarta.enterprise.context.ApplicationScoped;

@MyNonInheritedTransitiveStereotype
@ApplicationScoped
public class NonInheritedOnlyDirectTransitiveStereotype extends ServiceBase {
}
