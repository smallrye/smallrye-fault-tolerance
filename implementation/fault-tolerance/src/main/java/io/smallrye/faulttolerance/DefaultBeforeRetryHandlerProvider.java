package io.smallrye.faulttolerance;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Unmanaged;
import jakarta.inject.Inject;

import io.smallrye.faulttolerance.api.BeforeRetryHandler;
import io.smallrye.faulttolerance.api.CallContext;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;

/**
 * Default implementation of {@link BeforeRetryHandlerProvider}.
 */
@Dependent
public class DefaultBeforeRetryHandlerProvider implements BeforeRetryHandlerProvider {
    @Inject
    BeanManager beanManager;

    @Override
    public BeforeRetryHandler get(FaultToleranceOperation operation) {
        if (operation.hasBeforeRetry()) {
            return new BeforeRetryHandler() {
                @Override
                public void handle(CallContext context) {
                    Unmanaged<BeforeRetryHandler> unmanaged = new Unmanaged<>(beanManager,
                            (Class<BeforeRetryHandler>) operation.getBeforeRetry().value());
                    Unmanaged.UnmanagedInstance<BeforeRetryHandler> unmanagedInstance = unmanaged.newInstance();
                    BeforeRetryHandler handler = unmanagedInstance.produce().inject().postConstruct().get();
                    try {
                        handler.handle(context);
                    } finally {
                        // The instance exists to service a single invocation only
                        unmanagedInstance.preDestroy().dispose();
                    }
                }
            };
        }
        return null;
    }
}
