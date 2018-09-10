package io.smallrye.faulttolerance.async.requestcontext;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.weld.context.RequestContext;
import org.jboss.weld.context.unbound.Unbound;

import io.smallrye.faulttolerance.CommandListener;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;

@ApplicationScoped
public class RequestContextCommandListener implements CommandListener {

    @Unbound
    @Inject
    RequestContext requestContext;

    @Override
    public void beforeExecution(FaultToleranceOperation operation) {
        requestContext.activate();
    }

    @Override
    public void afterExecution(FaultToleranceOperation operation) {
        requestContext.invalidate();
        requestContext.deactivate();
    }

}
