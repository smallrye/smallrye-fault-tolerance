package io.smallrye.faulttolerance;

import com.netflix.hystrix.HystrixCommand;

import io.smallrye.faulttolerance.config.FaultToleranceOperation;

public abstract class BasicCommand extends HystrixCommand<Object> {

    protected BasicCommand(Setter setter) {
        super(setter);
    }

    abstract void setFailure(Throwable f);

    abstract FaultToleranceOperation getOperation();

}
