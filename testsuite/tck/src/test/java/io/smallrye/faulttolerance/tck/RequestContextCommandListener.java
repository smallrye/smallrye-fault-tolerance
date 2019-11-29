package io.smallrye.faulttolerance.tck;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.jboss.weld.context.RequestContext;
import org.jboss.weld.context.unbound.Unbound;
import org.jboss.weld.manager.BeanManagerImpl;

import io.smallrye.faulttolerance.CommandListener;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;

// a copy of RequestContextCommandListener from Thorntail
// see org.wildfly.swarm.microprofile.faulttolerance.deployment.RequestContextCommandListener
@ApplicationScoped
public class RequestContextCommandListener implements CommandListener {

    private final BeanManagerImpl beanManager;

    private final RequestContext requestContext;

    private final ThreadLocal<Boolean> isActivator;

    @Inject
    public RequestContextCommandListener(@Unbound RequestContext requestContext, BeanManagerImpl beanManager) {
        this.requestContext = requestContext;
        this.beanManager = beanManager;
        this.isActivator = new ThreadLocal<>();
    }

    @Override
    public void beforeExecution(FaultToleranceOperation operation) {
        // Note that Hystrix commands are executed on a dedicated thread where a CDI request context should not be active
        // TODO: maybe we should activate the context for any FT operation?
        if (operation.isAsync() && !beanManager.isContextActive(RequestScoped.class)) {
            requestContext.activate();
            isActivator.set(true);
        }
    }

    @Override
    public void afterExecution(FaultToleranceOperation operation) {
        if (Boolean.TRUE.equals(isActivator.get())) {
            try {
                requestContext.invalidate();
                requestContext.deactivate();
            } finally {
                isActivator.remove();
            }
        }
    }
}
