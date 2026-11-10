package io.smallrye.faulttolerance.retry.computed.delay.error;

import java.time.Duration;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;

import io.smallrye.faulttolerance.api.ComputedDelayHandler;

public class MyDummyComputedDelayHandler implements ComputedDelayHandler {
    @Override
    public Duration getDelay(ExecutionContext context) {
        return null;
    }
}
