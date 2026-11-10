package io.smallrye.faulttolerance.retry.computed.delay.error;

import jakarta.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.ComputedDelay;

@Dependent
@Retry
public class RetryOnClassComputedDelayOnMethodService {
    @ComputedDelay(MyDummyComputedDelayHandler.class)
    public void hello() {
    }
}
