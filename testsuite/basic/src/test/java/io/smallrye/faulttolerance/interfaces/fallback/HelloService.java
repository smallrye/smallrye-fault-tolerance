package io.smallrye.faulttolerance.interfaces.fallback;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class HelloService {
    @Inject
    @InterfaceBased
    private PublicHello publicHello;

    @Inject
    @InterfaceBased
    private PackagePrivateHello packagePrivateHello;

    public String hello() {
        return publicHello.hello() + " " + packagePrivateHello.hello();
    }
}
