package io.smallrye.faulttolerance.config;

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig
public interface BulkheadConfig extends Bulkhead, Config {
    @Override
    default void validate() {
        final String INVALID_BULKHEAD_ON = "Invalid @Bulkhead on ";

        if (value() < 1) {
            throw new FaultToleranceDefinitionException(INVALID_BULKHEAD_ON + method()
                    + ": value shouldn't be lower than 1");
        }
        if (waitingTaskQueue() < 1) {
            throw new FaultToleranceDefinitionException(INVALID_BULKHEAD_ON + method()
                    + ": waitingTaskQueue shouldn't be lower than 1");
        }

        try {
            Math.addExact(value(), waitingTaskQueue());
        } catch (ArithmeticException e) {
            throw new FaultToleranceDefinitionException(INVALID_BULKHEAD_ON + method()
                    + ": bulkhead capacity overflow, " + value() + " + " + waitingTaskQueue()
                    + " = " + (value() + waitingTaskQueue()));
        }
    }
}
