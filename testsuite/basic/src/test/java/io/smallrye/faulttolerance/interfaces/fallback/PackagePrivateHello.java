package io.smallrye.faulttolerance.interfaces.fallback;

import org.eclipse.microprofile.faulttolerance.Fallback;

@RegisterInterfaceBased
interface PackagePrivateHello {
    @Fallback(fallbackMethod = "fallback")
    String hello();

    default String fallback() {
        return "world!";
    }
}
