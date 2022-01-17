package io.smallrye.faulttolerance.fallback.causechain;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;

@ApplicationScoped
public class MyService {
    @Fallback(fallbackMethod = "fallback", skipOn = ExpectedOutcomeException.class, applyOn = IOException.class)
    public void hello(Exception e) throws Exception {
        throw e;
    }

    public void fallback(Exception ignored) {
    }
}
