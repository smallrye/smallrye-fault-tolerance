package io.smallrye.faulttolerance.reuse.mismatch.sync.vs.async;

import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.ApplyFaultTolerance;

@ApplicationScoped
public class MyService {
    @ApplyFaultTolerance("my-fault-tolerance")
    public CompletionStage<String> hello() {
        return null;
    }
}
