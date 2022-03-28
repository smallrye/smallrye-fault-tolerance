package io.smallrye.faulttolerance.defaultmethod;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class HelloService {
    @Inject
    @InterfaceBased
    private SimpleService service;

    public String hello() {
        return service.hello();
    }
}
