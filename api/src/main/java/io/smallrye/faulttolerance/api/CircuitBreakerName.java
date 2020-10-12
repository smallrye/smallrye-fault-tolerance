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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.smallrye.common.annotation.Experimental;

/**
 * A {@code @CircuitBreaker} method can be annotated {@code @CircuitBreakerName}
 * to provide a name for the circuit breaker associated with the annotated method.
 * This name can then be used with {@link CircuitBreakerMaintenance}.
 * <p>
 * It is an error if multiple {@code @CircuitBreaker} methods have the same {@code @CircuitBreakerName}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Experimental("first attempt at providing maintenance access to circuit breakers")
public @interface CircuitBreakerName {
    String value();
}
