package io.smallrye.faulttolerance.interfaces.fallback;

import org.eclipse.microprofile.faulttolerance.Fallback;

@RegisterInterfaceBased
interface PackagePrivatePing {
    @Fallback(fallbackMethod = "fallback")
    String ping();

    default String fallback() {
        return "Pong!";
    }
}
