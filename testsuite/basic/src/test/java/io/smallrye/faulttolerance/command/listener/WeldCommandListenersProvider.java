package io.smallrye.faulttolerance.command.listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import org.jboss.weld.inject.WeldInstance;
import org.jboss.weld.inject.WeldInstance.Handler;

import io.smallrye.faulttolerance.CommandListener;
import io.smallrye.faulttolerance.DefaultCommandListenersProvider;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;

@Priority(1)
@Alternative
@ApplicationScoped
public class WeldCommandListenersProvider extends DefaultCommandListenersProvider {

    @Inject
    WeldInstance<CommandListener> listeners;

    @Override
    public List<CommandListener> getCommandListeners() {
        if (listeners.isUnsatisfied()) {
            return null;
        }
        List<CommandListener> commandListeners = new ArrayList<>();
        for (Handler<CommandListener> handler : listeners.handlers()) {
            if (Dependent.class.equals(handler.getBean().getScope())) {
                // Wrap dependent listener
                commandListeners.add(new CommandListener() {

                    @Override
                    public void beforeExecution(FaultToleranceOperation operation) {
                        handler.get().beforeExecution(operation);
                    }

                    @Override
                    public void afterExecution(FaultToleranceOperation operation) {
                        handler.get().afterExecution(operation);
                        handler.destroy();
                    }

                    @Override
                    public int getPriority() {
                        return handler.get().getPriority();
                    }

                });
            } else {
                commandListeners.add(handler.get());
            }
        }
        Collections.sort(commandListeners);
        return commandListeners;
    }

}
