package io.smallrye.faulttolerance.basicconfig;

import org.eclipse.microprofile.faulttolerance.Bulkhead;

import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig
public interface BulkheadConfig extends Bulkhead, Config {
    @Override
    default void validate() {
        if (value() < 1) {
            throw fail("value", "shouldn't be lower than 1");
        }
        if (waitingTaskQueue() < 1) {
            throw fail("waitingTaskQueue", "shouldn't be lower than 1");
        }

        try {
            Math.addExact(value(), waitingTaskQueue());
        } catch (ArithmeticException e) {
            throw fail("bulkhead capacity overflow, " + value() + " + " + waitingTaskQueue()
                    + " = " + (value() + waitingTaskQueue()));
        }
    }
}
