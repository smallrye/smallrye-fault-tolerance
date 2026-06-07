package io.smallrye.faulttolerance.stereotype;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

@MyStereotype
@Retry(maxRetries = 5)
@ApplicationScoped
public class MethodOverridesClassAndDirectStereotype extends ServiceBase {
    @Override
    @Retry(maxRetries = 6)
    @Fallback(fallbackMethod = "fallback")
    public String hello() {
        return super.hello();
    }
}
