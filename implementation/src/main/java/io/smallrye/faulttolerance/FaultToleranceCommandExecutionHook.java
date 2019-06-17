package io.smallrye.faulttolerance;

import com.netflix.hystrix.HystrixInvokable;
import com.netflix.hystrix.strategy.executionhook.HystrixCommandExecutionHook;

public class FaultToleranceCommandExecutionHook extends HystrixCommandExecutionHook {

    @Override
    public <T> Exception onExecutionError(HystrixInvokable<T> commandInstance, Exception e) {
        if (commandInstance instanceof BasicCommand) {
            BasicCommand command = ((BasicCommand) commandInstance);
            command.setFailure(e);
        }
        return e;
    }

}
