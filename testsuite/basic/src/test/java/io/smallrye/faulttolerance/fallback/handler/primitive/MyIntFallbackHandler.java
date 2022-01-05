package io.smallrye.faulttolerance.fallback.handler.primitive;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

public class MyIntFallbackHandler implements FallbackHandler<Integer> {
    static boolean invoked = false;

    @Override
    public Integer handle(ExecutionContext executionContext) {
        invoked = true;
        return 42;
    }
}
