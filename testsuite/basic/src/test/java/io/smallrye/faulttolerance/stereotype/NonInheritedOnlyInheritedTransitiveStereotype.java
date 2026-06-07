package io.smallrye.faulttolerance.stereotype;

import jakarta.enterprise.context.ApplicationScoped;

// the superclass has a stereotype, but that isn't inherited
@ApplicationScoped
public class NonInheritedOnlyInheritedTransitiveStereotype extends ServiceBaseWithNonInheritedTransitiveStereotype {
}
