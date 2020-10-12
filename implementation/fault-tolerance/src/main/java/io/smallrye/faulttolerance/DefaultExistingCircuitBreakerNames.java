package io.smallrye.faulttolerance;

import java.util.Set;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DefaultExistingCircuitBreakerNames implements ExistingCircuitBreakerNames {
    private final Set<String> names;

    @Inject
    public DefaultExistingCircuitBreakerNames(BeanManager beanManager) {
        FaultToleranceExtension extension = beanManager.getExtension(FaultToleranceExtension.class);
        this.names = extension.getExistingCircuitBreakerNames();
    }

    @Override
    public boolean contains(String name) {
        return names.contains(name);
    }
}
