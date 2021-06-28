/*
 * Copyright 2017 Red Hat, Inc, and individual contributors.
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
package io.smallrye.faulttolerance.config;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.smallrye.faulttolerance.autoconfig.FaultToleranceMethod;
import io.smallrye.faulttolerance.autoconfig.MethodDescriptor;

/**
 * Fault tolerance operation metadata.
 *
 * @author Martin Kouba
 */
public class FaultToleranceOperation {

    public static FaultToleranceOperation create(FaultToleranceMethod method) {
        return new FaultToleranceOperation(method.beanClass, method.method,
                AsynchronousConfigImpl.create(method),
                BlockingConfigImpl.create(method),
                NonBlockingConfigImpl.create(method),
                BulkheadConfigImpl.create(method),
                CircuitBreakerConfigImpl.create(method),
                CircuitBreakerNameConfigImpl.create(method),
                FallbackConfigImpl.create(method),
                RetryConfigImpl.create(method),
                TimeoutConfigImpl.create(method));
    }

    private final Class<?> beanClass;

    private final MethodDescriptor methodDescriptor;

    private final AsynchronousConfig asynchronous;

    private final BlockingConfig blocking;

    private final NonBlockingConfig nonBlocking;

    private final BulkheadConfig bulkhead;

    private final CircuitBreakerConfig circuitBreaker;

    private final CircuitBreakerNameConfig circuitBreakerName;

    private final FallbackConfig fallback;

    private final RetryConfig retry;

    private final TimeoutConfig timeout;

    private FaultToleranceOperation(Class<?> beanClass,
            MethodDescriptor methodDescriptor,
            AsynchronousConfig asynchronous,
            BlockingConfig blocking,
            NonBlockingConfig nonBlocking,
            BulkheadConfig bulkhead,
            CircuitBreakerConfig circuitBreaker,
            CircuitBreakerNameConfig circuitBreakerName,
            FallbackConfig fallback,
            RetryConfig retry,
            TimeoutConfig timeout) {
        this.beanClass = beanClass;
        this.methodDescriptor = methodDescriptor;

        this.asynchronous = asynchronous;
        this.blocking = blocking;
        this.nonBlocking = nonBlocking;

        this.bulkhead = bulkhead;
        this.circuitBreaker = circuitBreaker;
        this.circuitBreakerName = circuitBreakerName;
        this.fallback = fallback;
        this.retry = retry;
        this.timeout = timeout;
    }

    public Class<?> getReturnType() {
        return methodDescriptor.returnType;
    }

    // whether @Asynchronous is present
    public boolean isAsync() {
        return asynchronous != null;
    }

    // whether @Blocking or @NonBlocking is present
    public boolean isAdditionalAsync() {
        return blocking != null || nonBlocking != null;
    }

    // whether thread offload is required based on presence or absence of @Blocking and @NonBlocking
    // if the guarded method doesn't return CompletionStage, this is meaningless
    public boolean isThreadOffloadRequired() {
        if (blocking != null) {
            return true;
        }
        if (nonBlocking != null) {
            return false;
        }

        // no @Blocking or @NonBlocking, we should offload to another thread as that's MP FT default
        return true;
    }

    public boolean hasBulkhead() {
        return bulkhead != null;
    }

    public BulkheadConfig getBulkhead() {
        return bulkhead;
    }

    public boolean hasCircuitBreaker() {
        return circuitBreaker != null;
    }

    public CircuitBreakerConfig getCircuitBreaker() {
        return circuitBreaker;
    }

    public boolean hasCircuitBreakerName() {
        return circuitBreakerName != null;
    }

    public CircuitBreakerNameConfig getCircuitBreakerName() {
        return circuitBreakerName;
    }

    public boolean hasFallback() {
        return fallback != null;
    }

    public FallbackConfig getFallback() {
        return fallback;
    }

    public boolean hasRetry() {
        return retry != null;
    }

    public RetryConfig getRetry() {
        return retry;
    }

    public boolean hasTimeout() {
        return timeout != null;
    }

    public TimeoutConfig getTimeout() {
        return timeout;
    }

    public String getName() {
        return beanClass.getCanonicalName() + "." + methodDescriptor.name;
    }

    public boolean isValid() {
        try {
            validate();
            return true;
        } catch (FaultToleranceDefinitionException e) {
            return false;
        }
    }

    /**
     * Throws {@link FaultToleranceDefinitionException} if validation fails.
     */
    public void validate() {
        if (asynchronous != null) {
            asynchronous.validate();
        }
        if (blocking != null) {
            blocking.validate();
        }
        if (nonBlocking != null) {
            nonBlocking.validate();
        }
        if (bulkhead != null) {
            bulkhead.validate();
        }
        if (circuitBreaker != null) {
            circuitBreaker.validate();
        }
        if (fallback != null) {
            fallback.validate();
        }
        if (retry != null) {
            retry.validate();
        }
        if (timeout != null) {
            timeout.validate();
        }
    }

    @Override
    public String toString() {
        return "FaultToleranceOperation[" + methodDescriptor + "]";
    }
}
