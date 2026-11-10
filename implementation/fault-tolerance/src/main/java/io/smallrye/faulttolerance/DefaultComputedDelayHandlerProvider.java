package io.smallrye.faulttolerance;

import java.time.Duration;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Unmanaged;
import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;

import io.smallrye.faulttolerance.api.ComputedDelayHandler;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;

/**
 * Default implementation of {@link ComputedDelayHandlerProvider}.
 */
@Dependent
public class DefaultComputedDelayHandlerProvider implements ComputedDelayHandlerProvider {
    @Inject
    BeanManager beanManager;

    @Override
    public ComputedDelayHandler get(FaultToleranceOperation operation) {
        if (operation.hasComputedDelay()) {
            //noinspection Convert2Lambda
            return new ComputedDelayHandler() {
                @Override
                public Duration getDelay(ExecutionContext context) {
                    Unmanaged<ComputedDelayHandler> unmanaged = new Unmanaged<>(beanManager,
                            (Class<ComputedDelayHandler>) operation.getComputedDelay().value());
                    Unmanaged.UnmanagedInstance<ComputedDelayHandler> unmanagedInstance = unmanaged.newInstance();
                    ComputedDelayHandler handler = unmanagedInstance.produce().inject().postConstruct().get();
                    try {
                        return handler.getDelay(context);
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
