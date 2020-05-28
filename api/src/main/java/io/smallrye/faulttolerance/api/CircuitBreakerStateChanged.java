/*
 * Copyright 202 Red Hat, Inc, and individual contributors.
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

import java.lang.reflect.Method;

import io.smallrye.common.annotation.Experimental;

/**
 * CDI event type that is fired when some circuit breaker changes its state.
 */
@Experimental("first attempt at providing a way to observe circuit breaker state")
public class CircuitBreakerStateChanged {
    public final Class<?> clazz;
    public final Method method;
    public final CircuitBreakerState targetState;

    public CircuitBreakerStateChanged(Class<?> clazz, Method method, CircuitBreakerState targetState) {
        this.clazz = clazz;
        this.method = method;
        this.targetState = targetState;
    }
}
