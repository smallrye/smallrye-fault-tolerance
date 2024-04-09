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

import static io.smallrye.faulttolerance.core.util.Durations.timeInMillis;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.faulttolerance.api.AlwaysOnException;
import io.smallrye.faulttolerance.api.ApplyFaultTolerance;
import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.faulttolerance.api.CustomBackoff;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import io.smallrye.faulttolerance.api.FibonacciBackoff;
import io.smallrye.faulttolerance.api.RateLimit;
import io.smallrye.faulttolerance.api.RetryWhen;
import io.smallrye.faulttolerance.autoconfig.Config;
import io.smallrye.faulttolerance.autoconfig.FaultToleranceMethod;
import io.smallrye.faulttolerance.autoconfig.MethodDescriptor;

/**
 * Fault tolerance operation metadata.
 */
public class FaultToleranceOperation {

    public static FaultToleranceOperation create(FaultToleranceMethod method) {
        return new FaultToleranceOperation(method.beanClass, method.method,
                ApplyFaultToleranceConfigImpl.create(method),
                AsynchronousConfigImpl.create(method),
                AsynchronousNonBlockingConfigImpl.create(method),
                BlockingConfigImpl.create(method),
                NonBlockingConfigImpl.create(method),
                BulkheadConfigImpl.create(method),
                CircuitBreakerConfigImpl.create(method),
                CircuitBreakerNameConfigImpl.create(method),
                FallbackConfigImpl.create(method),
                RateLimitConfigImpl.create(method),
                RetryConfigImpl.create(method),
                TimeoutConfigImpl.create(method),
                ExponentialBackoffConfigImpl.create(method),
                FibonacciBackoffConfigImpl.create(method),
                CustomBackoffConfigImpl.create(method),
                RetryWhenConfigImpl.create(method));
    }

    private final Class<?> beanClass;
    private final MethodDescriptor methodDescriptor;

    private final ApplyFaultToleranceConfig applyFaultTolerance;

    private final AsynchronousConfig asynchronous;
    private final AsynchronousNonBlockingConfig asynchronousNonBlocking;
    private final BlockingConfig blocking;
    private final NonBlockingConfig nonBlocking;

    private final BulkheadConfig bulkhead;
    private final CircuitBreakerConfig circuitBreaker;
    private final CircuitBreakerNameConfig circuitBreakerName;
    private final FallbackConfig fallback;
    private final RateLimitConfig rateLimit;
    private final RetryConfig retry;
    private final TimeoutConfig timeout;

    private final ExponentialBackoffConfig exponentialBackoff;
    private final FibonacciBackoffConfig fibonacciBackoff;
    private final CustomBackoffConfig customBackoff;
    private final RetryWhenConfig retryWhen;

    private FaultToleranceOperation(Class<?> beanClass,
            MethodDescriptor methodDescriptor,
            ApplyFaultToleranceConfig applyFaultTolerance,
            AsynchronousConfig asynchronous,
            AsynchronousNonBlockingConfig asynchronousNonBlocking,
            BlockingConfig blocking,
            NonBlockingConfig nonBlocking,
            BulkheadConfig bulkhead,
            CircuitBreakerConfig circuitBreaker,
            CircuitBreakerNameConfig circuitBreakerName,
            FallbackConfig fallback,
            RateLimitConfig rateLimit,
            RetryConfig retry,
            TimeoutConfig timeout,
            ExponentialBackoffConfig exponentialBackoff,
            FibonacciBackoffConfig fibonacciBackoff,
            CustomBackoffConfig customBackoff,
            RetryWhenConfig retryWhen) {
        this.beanClass = beanClass;
        this.methodDescriptor = methodDescriptor;

        this.applyFaultTolerance = applyFaultTolerance;

        this.asynchronous = asynchronous;
        this.asynchronousNonBlocking = asynchronousNonBlocking;
        this.blocking = blocking;
        this.nonBlocking = nonBlocking;

        this.bulkhead = bulkhead;
        this.circuitBreaker = circuitBreaker;
        this.circuitBreakerName = circuitBreakerName;
        this.fallback = fallback;
        this.rateLimit = rateLimit;
        this.retry = retry;
        this.timeout = timeout;

        this.exponentialBackoff = exponentialBackoff;
        this.fibonacciBackoff = fibonacciBackoff;
        this.customBackoff = customBackoff;
        this.retryWhen = retryWhen;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public MethodDescriptor getMethodDescriptor() {
        return methodDescriptor;
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

    public Asynchronous getAsynchronous() {
        return asynchronous;
    }

    public boolean hasAsynchronousNonBlocking() {
        return asynchronousNonBlocking != null;
    }

    public AsynchronousNonBlocking getAsynchronousNonBlocking() {
        return asynchronousNonBlocking;
    }

    public boolean hasBlocking() {
        return blocking != null;
    }

    public Blocking getBlocking() {
        return blocking;
    }

    public boolean hasNonBlocking() {
        return nonBlocking != null;
    }

    public NonBlocking getNonBlocking() {
        return nonBlocking;
    }

    // if the guarded method doesn't return CompletionStage, this is meaningless
    public boolean isThreadOffloadRequired() {
        if (blocking == null && nonBlocking == null) {
            if (asynchronousNonBlocking != null && asynchronousNonBlocking.isOnMethod()) {
                return false;
            }
            if (asynchronous != null && asynchronous.isOnMethod()) {
                return true;
            }

            if (asynchronousNonBlocking != null) {
                return false;
            }
            if (asynchronous != null) {
                return true;
            }

            // in spec compatible mode, one of the conditions above always holds
            // in spec non-compatible mode, we can just always return `false`
            // because `isThreadOffloadRequired` is never called when the return type
            // isn't `CompletionStage`
            return false;
        }

        // the code below is meant to be deleted when support for `@Blocking` and `@NonBlocking` is removed

        if (blocking != null && blocking.isOnMethod()) {
            return true;
        }
        if (nonBlocking != null && nonBlocking.isOnMethod()) {
            return false;
        }
        if (asynchronousNonBlocking != null && asynchronousNonBlocking.isOnMethod()) {
            return false;
        }

        if (blocking != null) {
            return true;
        }
        if (nonBlocking != null) {
            return false;
        }
        if (asynchronousNonBlocking != null) {
            return false;
        }

        if (asynchronous != null) {
            return true;
        }

        // in spec compatible mode, one of the conditions above always holds
        // in spec non-compatible mode, we can just always return `false`
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

    public boolean hasRateLimit() {
        return rateLimit != null;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
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

    public boolean hasRetryWhen() {
        return retryWhen != null;
    }

    public RetryWhen getRetryWhen() {
        return retryWhen;
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
        if (asynchronousNonBlocking != null) {
            asynchronousNonBlocking.validate();
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
        if (rateLimit != null) {
            rateLimit.validate();
        }
        if (retry != null) {
            retry.validate();
        }
        if (timeout != null) {
            timeout.validate();
        }

        validateRetryBackoff();
        validateRetryWhen();
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
            long retryMaxDuration = timeInMillis(retry.maxDuration(), retry.durationUnit());
            if (retryMaxDuration > 0) {
                if (exponentialBackoff != null) {
                    long maxDelay = timeInMillis(exponentialBackoff.maxDelay(), exponentialBackoff.maxDelayUnit());
                    if (retryMaxDuration <= maxDelay) {
                        throw new FaultToleranceDefinitionException("Invalid @ExponentialBackoff on " + methodDescriptor
                                + ": @Retry.maxDuration should be greater than maxDelay");
                    }
                }

                if (fibonacciBackoff != null) {
                    long maxDelay = timeInMillis(fibonacciBackoff.maxDelay(), fibonacciBackoff.maxDelayUnit());
                    if (retryMaxDuration <= maxDelay) {
                        throw new FaultToleranceDefinitionException("Invalid @FibonacciBackoff on " + methodDescriptor
                                + ": @Retry.maxDuration should be greater than maxDelay");
                    }
                }
            }
        }
    }

    private void validateRetryWhen() {
        if (retryWhen == null) {
            return;
        }

        retryWhen.validate();

        if (retry == null) {
            throw new FaultToleranceDefinitionException("Invalid @RetryWhen on " + methodDescriptor + ": missing @Retry");
        }

        if (retryWhen.exception() != AlwaysOnException.class) {
            if (retry.abortOn().length != 0) {
                throw new FaultToleranceDefinitionException("Invalid @RetryWhen.exception on " + methodDescriptor
                        + ": must not be combined with @Retry.abortOn");
            }
            if (retry.retryOn().length != 1 || retry.retryOn()[0] != Exception.class) {
                throw new FaultToleranceDefinitionException("Invalid @RetryWhen.exception on " + methodDescriptor
                        + ": must not be combined with @Retry.retryOn");
            }
        }
    }

    /**
     * Ensures all configuration of this fault tolerance operation is loaded. Subsequent method invocations
     * on this instance are guaranteed to not touch MP Config.
     */
    public void materialize() {
        if (applyFaultTolerance != null) {
            applyFaultTolerance.materialize();
        }

        if (asynchronous != null) {
            asynchronous.materialize();
        }
        if (asynchronousNonBlocking != null) {
            asynchronousNonBlocking.materialize();
        }
        if (blocking != null) {
            blocking.materialize();
        }
        if (nonBlocking != null) {
            nonBlocking.materialize();
        }

        if (bulkhead != null) {
            bulkhead.materialize();
        }
        if (circuitBreaker != null) {
            circuitBreaker.materialize();
        }
        if (circuitBreakerName != null) {
            circuitBreakerName.materialize();
        }
        if (fallback != null) {
            fallback.materialize();
        }
        if (rateLimit != null) {
            rateLimit.materialize();
        }
        if (retry != null) {
            retry.materialize();
        }
        if (timeout != null) {
            timeout.materialize();
        }

        if (exponentialBackoff != null) {
            exponentialBackoff.materialize();
        }
        if (fibonacciBackoff != null) {
            fibonacciBackoff.materialize();
        }
        if (customBackoff != null) {
            customBackoff.materialize();
        }
        if (retryWhen != null) {
            retryWhen.materialize();
        }
    }

    @Override
    public String toString() {
        return "FaultToleranceOperation[" + methodDescriptor + "]";
    }
}
