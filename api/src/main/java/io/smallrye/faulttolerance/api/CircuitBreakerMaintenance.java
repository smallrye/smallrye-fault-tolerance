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
 * Allows reading current circuit breaker state and reseting it to initial (closed) state.
 * The circuit breaker must be given a name using {@link CircuitBreakerName @CircuitBreakerName},
 * and then {@code CircuitBreakerMaintenance} can be {@code @Inject}-ed.
 * <p>
 * It is an error if {@code CircuitBreakerMaintenance} is injected
 * without using the {@code @CircuitBreakerName} qualifier.
 */
@Experimental("first attempt at providing maintenance access to circuit breakers")
public interface CircuitBreakerMaintenance {
    /**
     * Returns current state of the circuit breaker.
     * Note that there's no guarantee that the circuit breaker will stay in that state for any time,
     * so this method is only useful for monitoring.
     */
    CircuitBreakerState currentState();

    /**
     * Resets the circuit breaker to the initial (that is, closed) state.
     * This method should not be used regularly, it's mainly meant for testing (to isolate individual tests)
     * and perhaps emergency maintenance tasks.
     */
    void reset();
}
