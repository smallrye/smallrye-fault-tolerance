package io.smallrye.faulttolerance.defaultmethod;

import org.eclipse.microprofile.faulttolerance.Fallback;

@RegisterInterfaceBased
public interface SimpleService {
    @Fallback(fallbackMethod = "fallback")
    String hello();

    default String fallback() {
        return "Hello, world!";
    }
}
