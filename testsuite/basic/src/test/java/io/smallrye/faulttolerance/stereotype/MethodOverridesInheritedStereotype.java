package io.smallrye.faulttolerance.stereotype;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

@ApplicationScoped
public class MethodOverridesInheritedStereotype extends ServiceBaseWithStereotype {
    @Override
    @Retry(maxRetries = 6)
    @Fallback(fallbackMethod = "fallback")
    public String hello() {
        return super.hello();
    }
}
