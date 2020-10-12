/*
 * Copyright 2020 Red Hat, Inc, and individual contributors.
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
package io.smallrye.faulttolerance.api;

import io.smallrye.common.annotation.Experimental;

/**
 * Allows reading current state of circuit breakers and reseting them to the initial (closed) state.
 * To access a specific circuit breaker, it must be given a name using {@link CircuitBreakerName @CircuitBreakerName}.
 */
@Experimental("first attempt at providing maintenance access to circuit breakers")
public interface CircuitBreakerMaintenance {
    /**
     * Returns current state of the circuit breaker with given {@code name}.
     * Note that there's no guarantee that the circuit breaker will stay in that state for any time,
     * so this method is only useful for monitoring.
     * <p>
     * It is an error to use a {@code name} that wasn't registered using {@code @CircuitBreakerName}.
     */
    CircuitBreakerState currentState(String name);

    /**
     * Resets the circuit breaker with given {@code name} to the initial (closed) state.
     * <p>
     * This method should not be used regularly, it's mainly meant for testing (to isolate individual tests)
     * and perhaps emergency maintenance tasks.
     * <p>
     * It is an error to use a {@code name} that wasn't registered using {@code @CircuitBreakerName}.
     */
    void reset(String name);

    /**
     * Resets all circuit breakers (including those without a name) in the application to the initial (closed) state.
     * <p>
     * This method should not be used regularly, it's mainly meant for testing (to isolate individual tests)
     * and perhaps emergency maintenance tasks.
     */
    void resetAll();
}
