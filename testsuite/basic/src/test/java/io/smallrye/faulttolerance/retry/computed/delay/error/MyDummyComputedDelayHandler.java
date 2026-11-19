package io.smallrye.faulttolerance.retry.computed.delay.error;

import java.time.Duration;

import io.smallrye.faulttolerance.api.CallContext;
import io.smallrye.faulttolerance.api.ComputedDelayHandler;

public class MyDummyComputedDelayHandler implements ComputedDelayHandler {
    @Override
    public Duration getDelay(CallContext context) {
        return null;
    }
}
