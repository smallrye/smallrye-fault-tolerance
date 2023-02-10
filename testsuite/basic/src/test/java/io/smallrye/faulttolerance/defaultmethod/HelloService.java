package io.smallrye.faulttolerance.defaultmethod;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class HelloService {
    @Inject
    @InterfaceBased
    private SimpleService service;

    public String hello() {
        return service.hello();
    }
}
