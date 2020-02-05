package io.smallrye.faulttolerance.internal;

import javax.enterprise.context.control.RequestContextController;
import javax.enterprise.inject.spi.CDI;

public class DefaultRequestContextControllerProvider implements RequestContextControllerProvider {
    @Override
    public RequestContextController get() {
        return CDI.current().select(RequestContextController.class).get();
    }
}
