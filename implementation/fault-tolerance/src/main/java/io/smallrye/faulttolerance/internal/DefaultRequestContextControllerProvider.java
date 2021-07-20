package io.smallrye.faulttolerance.internal;

import jakarta.enterprise.context.control.RequestContextController;
import jakarta.enterprise.inject.spi.CDI;

public class DefaultRequestContextControllerProvider implements RequestContextControllerProvider {
    @Override
    public RequestContextController get() {
        return CDI.current().select(RequestContextController.class).get();
    }
}
