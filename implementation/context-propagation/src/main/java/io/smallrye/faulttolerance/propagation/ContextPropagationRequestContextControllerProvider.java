package io.smallrye.faulttolerance.propagation;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.control.RequestContextController;

import io.smallrye.faulttolerance.internal.RequestContextControllerProvider;

public class ContextPropagationRequestContextControllerProvider implements RequestContextControllerProvider {
    private final RequestContextController dummy = new RequestContextController() {
        @Override
        public boolean activate() {
            return false;
        }

        @Override
        public void deactivate() throws ContextNotActiveException {
        }
    };

    @Override
    public RequestContextController get() {
        return dummy;
    }
}
