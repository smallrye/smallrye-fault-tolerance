package io.smallrye.faulttolerance.internal;

import java.util.ServiceLoader;

import javax.enterprise.context.control.RequestContextController;

/**
 * This is <b>not</b> a public SPI, it's only meant to be used internally.
 */
public interface RequestContextControllerProvider {
    RequestContextController get();

    static RequestContextControllerProvider load() {
        for (RequestContextControllerProvider found : ServiceLoader.load(RequestContextControllerProvider.class)) {
            return found;
        }
        return new DefaultRequestContextControllerProvider();
    }
}
