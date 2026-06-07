package io.smallrye.faulttolerance.stereotype;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

@MyTransitiveStereotype
@Retry(maxRetries = 5)
@ApplicationScoped
public class MethodOverridesClassAndDirectTransitiveStereotype extends ServiceBase {
    @Override
    @Retry(maxRetries = 6)
    @Fallback(fallbackMethod = "fallback")
    public String hello() {
        return super.hello();
    }
}
