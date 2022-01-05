package io.smallrye.faulttolerance.fallback.handler.primitive;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

public class MyVoidFallbackHandler implements FallbackHandler<Void> {
    static boolean invoked = false;

    @Override
    public Void handle(ExecutionContext executionContext) {
        invoked = true;
        return null;
    }
}
