package io.smallrye.faulttolerance.reuse.missing;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.ApplyFaultTolerance;

@ApplicationScoped
public class MyService {
    @ApplyFaultTolerance("my-fault-tolerance")
    public String hello() {
        return null;
    }
}
