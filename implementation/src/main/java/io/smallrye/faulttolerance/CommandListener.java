/*
 * Copyright 2018 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.faulttolerance;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;

import io.smallrye.faulttolerance.config.FaultToleranceOperation;

/**
 * Any bean which implements this listener can be used to perfom actions before and after a Hystrix command that wraps a FT
 * operation is executed. The bean should be {@link Dependent} or {@link ApplicationScoped}. Note that a contextual instance
 * of this bean is obtained for each command execution.
 *
 * @author Martin Kouba
 * @see SimpleCommand#run()
 * @see CommandListenersProvider
 */
public interface CommandListener extends Comparable<CommandListener> {

    /**
     * Should not throw an exception.
     *
     * @param operation The fault tolerance operation metadata
     */
    default void beforeExecution(FaultToleranceOperation operation) {
    }

    /**
     * Should not throw an exception.
     *
     * @param operation The fault tolerance operation metadata
     */
    default void afterExecution(FaultToleranceOperation operation) {
    }

    /**
     * {@link #beforeExecution(FaultToleranceOperation)} of listeners with smaller priority values are called first.
     * {@link #afterExecution(FaultToleranceOperation)} is invoked in reverse order.
     *
     * @return the priority
     */
    default int getPriority() {
        return 1000;
    }

    @Override
    default int compareTo(CommandListener o) {
        return Integer.compare(getPriority(), o.getPriority());
    }

}
