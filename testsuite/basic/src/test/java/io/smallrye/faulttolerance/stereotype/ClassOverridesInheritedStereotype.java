package io.smallrye.faulttolerance.stereotype;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

@Retry(maxRetries = 5)
@ApplicationScoped
public class ClassOverridesInheritedStereotype extends ServiceBaseWithStereotype {
}
