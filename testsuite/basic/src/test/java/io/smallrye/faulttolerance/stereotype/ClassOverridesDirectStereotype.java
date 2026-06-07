package io.smallrye.faulttolerance.stereotype;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

@MyStereotype
@Retry(maxRetries = 5)
@ApplicationScoped
public class ClassOverridesDirectStereotype extends ServiceBase {
}
