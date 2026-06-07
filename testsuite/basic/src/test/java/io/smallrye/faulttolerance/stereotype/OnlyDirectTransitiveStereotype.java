package io.smallrye.faulttolerance.stereotype;

import jakarta.enterprise.context.ApplicationScoped;

@MyTransitiveStereotype
@ApplicationScoped
public class OnlyDirectTransitiveStereotype extends ServiceBase {
}
