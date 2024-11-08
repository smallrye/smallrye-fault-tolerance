package io.smallrye.faulttolerance.reuse.missing;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.ApplyGuard;

@ApplicationScoped
public class MyService {
    @ApplyGuard("my-fault-tolerance")
    public String hello() {
        return null;
    }
}
