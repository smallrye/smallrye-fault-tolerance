package io.smallrye.faulttolerance.stereotype;

import jakarta.enterprise.context.ApplicationScoped;

@MyNonInheritedStereotype
@ApplicationScoped
public class NonInheritedOnlyDirectStereotype extends ServiceBase {
}
