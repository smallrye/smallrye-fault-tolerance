package io.smallrye.faulttolerance.interfaces.fallback;

import org.eclipse.microprofile.faulttolerance.Fallback;

@RegisterInterfaceBased
public interface PublicHello {
    @Fallback(fallbackMethod = "fallback")
    String hello();

    default String fallback() {
        return "Hello,";
    }
}
