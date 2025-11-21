package io.smallrye.faulttolerance.interfaces.fallback;

import org.eclipse.microprofile.faulttolerance.Fallback;

@RegisterInterfaceBased
public interface PublicPing {
    @Fallback(fallbackMethod = "fallback")
    String ping();

    private String fallback() {
        return "Ping!";
    }
}
