package io.smallrye.faulttolerance;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Unmanaged;
import javax.inject.Inject;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

import io.smallrye.faulttolerance.config.FallbackConfig;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;

/**
 * Default implementation of {@link FallbackHandlerProvider}.
 *
 * @author Martin Kouba
 */
@Dependent
public class DefaultFallbackHandlerProvider implements FallbackHandlerProvider {

    @Inject
    BeanManager beanManager;

    @Override
    public <T> FallbackHandler<T> get(FaultToleranceOperation operation) {
        if (operation.hasFallback()) {
            //noinspection Convert2Lambda
            return new FallbackHandler<T>() {
                @Override
                public T handle(ExecutionContext context) {
                    Unmanaged<FallbackHandler<T>> unmanaged = new Unmanaged<>(beanManager,
                            operation.getFallback().get(FallbackConfig.VALUE));
                    Unmanaged.UnmanagedInstance<FallbackHandler<T>> unmanagedInstance = unmanaged.newInstance();
                    FallbackHandler<T> handler = unmanagedInstance.produce().inject().postConstruct().get();
                    try {
                        return handler.handle(context);
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
