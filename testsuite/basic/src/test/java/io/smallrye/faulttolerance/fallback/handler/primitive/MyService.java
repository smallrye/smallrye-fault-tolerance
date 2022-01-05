package io.smallrye.faulttolerance.fallback.handler.primitive;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;

@ApplicationScoped
public class MyService {
    @Fallback(MyVoidFallbackHandler.class)
    public void doSomething() {
        throw new RuntimeException();
    }

    @Fallback(MyIntFallbackHandler.class)
    public int returnSomething() {
        throw new RuntimeException();
    }
}
