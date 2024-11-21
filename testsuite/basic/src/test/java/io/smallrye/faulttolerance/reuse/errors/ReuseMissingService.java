package io.smallrye.faulttolerance.reuse.errors;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.ApplyGuard;

@ApplicationScoped
public class ReuseMissingService {
    @ApplyGuard("my-fault-tolerance")
    public String hello() {
        return null;
    }
}
