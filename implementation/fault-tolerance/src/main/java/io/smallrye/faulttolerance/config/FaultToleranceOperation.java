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

import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.smallrye.faulttolerance.api.ApplyFaultTolerance;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.faulttolerance.api.CustomBackoff;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import io.smallrye.faulttolerance.api.FibonacciBackoff;
import io.smallrye.faulttolerance.autoconfig.Config;
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
                ApplyFaultToleranceConfigImpl.create(method),
                AsynchronousConfigImpl.create(method),
                BlockingConfigImpl.create(method),
                NonBlockingConfigImpl.create(method),
                BulkheadConfigImpl.create(method),
                CircuitBreakerConfigImpl.create(method),
                CircuitBreakerNameConfigImpl.create(method),
                FallbackConfigImpl.create(method),
                RetryConfigImpl.create(method),
                TimeoutConfigImpl.create(method),
                ExponentialBackoffConfigImpl.create(method),
                FibonacciBackoffConfigImpl.create(method),
                CustomBackoffConfigImpl.create(method));
    }

    private final Class<?> beanClass;

    private final MethodDescriptor methodDescriptor;

    private final ApplyFaultToleranceConfig applyFaultTolerance;

    private final AsynchronousConfig asynchronous;

    private final BlockingConfig blocking;

    private final NonBlockingConfig nonBlocking;

    private final BulkheadConfig bulkhead;

    private final CircuitBreakerConfig circuitBreaker;

    private final CircuitBreakerNameConfig circuitBreakerName;

    private final FallbackConfig fallback;

    private final RetryConfig retry;

    private final TimeoutConfig timeout;

    private final ExponentialBackoffConfig exponentialBackoff;

    private final FibonacciBackoffConfig fibonacciBackoff;

    private final CustomBackoffConfig customBackoff;

    private FaultToleranceOperation(Class<?> beanClass,
            MethodDescriptor methodDescriptor,
            ApplyFaultToleranceConfig applyFaultTolerance,
            AsynchronousConfig asynchronous,
            BlockingConfig blocking,
            NonBlockingConfig nonBlocking,
            BulkheadConfig bulkhead,
            CircuitBreakerConfig circuitBreaker,
            CircuitBreakerNameConfig circuitBreakerName,
            FallbackConfig fallback,
            RetryConfig retry,
            TimeoutConfig timeout,
            ExponentialBackoffConfig exponentialBackoff,
            FibonacciBackoffConfig fibonacciBackoff,
            CustomBackoffConfig customBackoff) {
        this.beanClass = beanClass;
        this.methodDescriptor = methodDescriptor;

        this.applyFaultTolerance = applyFaultTolerance;

        this.asynchronous = asynchronous;
        this.blocking = blocking;
        this.nonBlocking = nonBlocking;

        this.bulkhead = bulkhead;
        this.circuitBreaker = circuitBreaker;
        this.circuitBreakerName = circuitBreakerName;
        this.fallback = fallback;
        this.retry = retry;
        this.timeout = timeout;

        this.exponentialBackoff = exponentialBackoff;
        this.fibonacciBackoff = fibonacciBackoff;
        this.customBackoff = customBackoff;
    }

    public Class<?>[] getParameterTypes() {
        return methodDescriptor.parameterTypes;
    }

    public Class<?> getReturnType() {
        return methodDescriptor.returnType;
    }

    public boolean hasApplyFaultTolerance() {
        return applyFaultTolerance != null;
    }

    public ApplyFaultTolerance getApplyFaultTolerance() {
        return applyFaultTolerance;
    }

    public boolean hasAsynchronous() {
        return asynchronous != null;
    }

    public boolean hasBlocking() {
        return blocking != null;
    }

    public boolean hasNonBlocking() {
        return nonBlocking != null;
    }

    // if the guarded method doesn't return CompletionStage, this is meaningless
    public boolean isThreadOffloadRequired() {
        if (blocking != null && blocking.isOnMethod()) {
            return true;
        }
        if (nonBlocking != null && nonBlocking.isOnMethod()) {
            return false;
        }

        if (blocking != null) {
            return true;
        }
        if (nonBlocking != null) {
            return false;
        }

        if (asynchronous != null) {
            return true;
        }

        // in a spec compatible mode, one of the conditions above always holds
        // in a spec non-compatible mode, we can just always return `false`
        // because `isThreadOffloadRequired` is never called when the return type
        // isn't `CompletionStage`
        return false;
    }

    public boolean hasBulkhead() {
        return bulkhead != null;
    }

    public Bulkhead getBulkhead() {
        return bulkhead;
    }

    public boolean hasCircuitBreaker() {
        return circuitBreaker != null;
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    public boolean hasCircuitBreakerName() {
        return circuitBreakerName != null;
    }

    public CircuitBreakerName getCircuitBreakerName() {
        return circuitBreakerName;
    }

    public boolean hasFallback() {
        return fallback != null;
    }

    public Fallback getFallback() {
        return fallback;
    }

    public boolean hasRetry() {
        return retry != null;
    }

    public Retry getRetry() {
        return retry;
    }

    public boolean hasTimeout() {
        return timeout != null;
    }

    public Timeout getTimeout() {
        return timeout;
    }

    public boolean hasExponentialBackoff() {
        return exponentialBackoff != null;
    }

    public ExponentialBackoff getExponentialBackoff() {
        return exponentialBackoff;
    }

    public boolean hasFibonacciBackoff() {
        return fibonacciBackoff != null;
    }

    public FibonacciBackoff getFibonacciBackoff() {
        return fibonacciBackoff;
    }

    public boolean hasCustomBackoff() {
        return customBackoff != null;
    }

    public CustomBackoff getCustomBackoff() {
        return customBackoff;
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
        if (applyFaultTolerance != null) {
            applyFaultTolerance.validate();
        }

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

        validateRetryBackoff();
    }

    private void validateRetryBackoff() {
        Set<Class<? extends Annotation>> backoffAnnotations = new HashSet<>();

        for (Config cfg : Arrays.asList(exponentialBackoff, fibonacciBackoff, customBackoff)) {
            if (cfg != null) {
                cfg.validate();
                if (retry == null) {
                    throw new FaultToleranceDefinitionException("Invalid @" + cfg.annotationType().getSimpleName()
                            + " on " + methodDescriptor + ": missing @Retry");
                }
                backoffAnnotations.add(cfg.annotationType());
            }
        }

        if (backoffAnnotations.size() > 1) {
            throw new FaultToleranceDefinitionException("More than one backoff defined for " + methodDescriptor
                    + ": " + backoffAnnotations);
        }

        if (retry != null) {
            long retryMaxDuration = Duration.of(retry.maxDuration(), retry.durationUnit()).toMillis();
            if (retryMaxDuration > 0) {
                if (exponentialBackoff != null) {
                    long maxDelay = Duration.of(exponentialBackoff.maxDelay(), exponentialBackoff.maxDelayUnit()).toMillis();
                    if (retryMaxDuration <= maxDelay) {
                        throw new FaultToleranceDefinitionException("Invalid @ExponentialBackoff on " + methodDescriptor
                                + ": @Retry.maxDuration should be greater than maxDelay");
                    }
                }

                if (fibonacciBackoff != null) {
                    long maxDelay = Duration.of(fibonacciBackoff.maxDelay(), fibonacciBackoff.maxDelayUnit()).toMillis();
                    if (retryMaxDuration <= maxDelay) {
                        throw new FaultToleranceDefinitionException("Invalid @FibonacciBackoff on " + methodDescriptor
                                + ": @Retry.maxDuration should be greater than maxDelay");
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return "FaultToleranceOperation[" + methodDescriptor + "]";
    }
}
