package io.smallrye.faulttolerance.interfaces.fallback;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PingService {
    @Inject
    @InterfaceBased
    private PublicPing publicPing;

    @Inject
    @InterfaceBased
    private PackagePrivatePing packagePrivatePing;

    public String ping() {
        return publicPing.ping() + " " + packagePrivatePing.ping();
    }
}
