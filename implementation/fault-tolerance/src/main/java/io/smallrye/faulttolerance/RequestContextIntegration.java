package io.smallrye.faulttolerance;

import jakarta.enterprise.context.control.RequestContextController;
import jakarta.inject.Singleton;

import io.smallrye.faulttolerance.internal.RequestContextControllerProvider;

@Singleton
public class RequestContextIntegration {
    private final RequestContextControllerProvider provider = RequestContextControllerProvider.load();

    public RequestContextController get() {
        return provider.get();
    }
}
