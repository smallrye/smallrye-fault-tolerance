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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.faulttolerance.SpecCompatibility;
import io.smallrye.faulttolerance.api.AlwaysOnException;
import io.smallrye.faulttolerance.api.ApplyFaultTolerance;
import io.smallrye.faulttolerance.api.ApplyGuard;
import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;
import io.smallrye.faulttolerance.api.BeforeRetry;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.faulttolerance.api.CustomBackoff;
import io.smallrye.faulttolerance.api.RetryWhen;
import io.smallrye.faulttolerance.autoconfig.Config;
import io.smallrye.faulttolerance.autoconfig.FaultToleranceMethod;
import io.smallrye.faulttolerance.autoconfig.MethodDescriptor;
import io.smallrye.faulttolerance.basicconfig.BasicFaultToleranceOperation;
import io.smallrye.faulttolerance.internal.FallbackMethodCandidates;

/**
 * Fault tolerance operation metadata. Used only for declarative fault tolerance.
 */
public class FaultToleranceOperation extends BasicFaultToleranceOperation {
    private final Class<?> beanClass;
    private final MethodDescriptor methodDescriptor;

    private final ApplyFaultToleranceConfig applyFaultTolerance;
    private final ApplyGuardConfig applyGuard;

    private final AsynchronousConfig asynchronous;
    private final AsynchronousNonBlockingConfig asynchronousNonBlocking;
    private final BlockingConfig blocking;
    private final NonBlockingConfig nonBlocking;

    private final CircuitBreakerNameConfig circuitBreakerName;
    private final FallbackConfig fallback;

    private final CustomBackoffConfig customBackoff;
    private final RetryWhenConfig retryWhen;
    private final BeforeRetryConfig beforeRetry;

    private final Method fallbackMethod;
    private final List<Method> fallbackMethodsWithExceptionParameter;
    private final Method beforeRetryMethod;

    public FaultToleranceOperation(FaultToleranceMethod method) {
        super(method);

        this.beanClass = method.beanClass;
        this.methodDescriptor = method.method;

        this.applyFaultTolerance = ApplyFaultToleranceConfigImpl.create(method);
        this.applyGuard = ApplyGuardConfigImpl.create(method);

        this.asynchronous = AsynchronousConfigImpl.create(method);
        this.asynchronousNonBlocking = AsynchronousNonBlockingConfigImpl.create(method);
        this.blocking = BlockingConfigImpl.create(method);
        this.nonBlocking = NonBlockingConfigImpl.create(method);

        this.circuitBreakerName = CircuitBreakerNameConfigImpl.create(method);
        this.fallback = FallbackConfigImpl.create(method);

        this.customBackoff = CustomBackoffConfigImpl.create(method);
        this.retryWhen = RetryWhenConfigImpl.create(method);
        this.beforeRetry = BeforeRetryConfigImpl.create(method);

        if (method.fallbackMethod != null) {
            try {
                this.fallbackMethod = SecurityActions.setAccessible(method.fallbackMethod.reflect());
            } catch (NoSuchMethodException e) {
                throw new FaultToleranceDefinitionException(e);
            }
        } else {
            this.fallbackMethod = null;
        }

        if (method.fallbackMethodsWithExceptionParameter != null) {
            List<Method> result = new ArrayList<>();
            for (MethodDescriptor m : method.fallbackMethodsWithExceptionParameter) {
                try {
                    result.add(SecurityActions.setAccessible(m.reflect()));
                } catch (NoSuchMethodException e) {
                    throw new FaultToleranceDefinitionException(e);
                }
            }
            this.fallbackMethodsWithExceptionParameter = result;
        } else {
            this.fallbackMethodsWithExceptionParameter = null;
        }

        if (method.beforeRetryMethod != null) {
            try {
                this.beforeRetryMethod = SecurityActions.setAccessible(method.beforeRetryMethod.reflect());
            } catch (NoSuchMethodException e) {
                throw new FaultToleranceDefinitionException(e);
            }
        } else {
            this.beforeRetryMethod = null;
        }
    }

    public String getName() {
        return beanClass.getCanonicalName() + "." + methodDescriptor.name;
    }

    // ---

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

    public boolean hasApplyGuard() {
        return applyGuard != null;
    }

    public ApplyGuard getApplyGuard() {
        return applyGuard;
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

    public boolean hasBeforeRetry() {
        return beforeRetry != null;
    }

    public BeforeRetry getBeforeRetry() {
        return beforeRetry;
    }

    public Method getFallbackMethod() {
        return fallbackMethod;
    }

    public List<Method> getFallbackMethodsWithExceptionParameter() {
        return fallbackMethodsWithExceptionParameter;
    }

    public Method getBeforeRetryMethod() {
        return beforeRetryMethod;
    }

    /**
     * Throws {@link FaultToleranceDefinitionException} if validation fails.
     */
    @Override
    public void validate() {
        super.validate();

        if (applyFaultTolerance != null) {
            applyFaultTolerance.validate();
        }
        if (applyGuard != null) {
            applyGuard.validate();
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

        if (circuitBreakerName != null) {
            circuitBreakerName.validate();
        }
        if (fallback != null) {
            fallback.validate();
        }

        validateApplyGuard();
        validateFallback();
        validateRetryWhen();
        validateBeforeRetry();
    }

    private void validateApplyGuard() {
        if (applyFaultTolerance != null && applyGuard != null) {
            throw new FaultToleranceDefinitionException(
                    "Both @ApplyFaultTolerance and @ApplyGuard present on " + description);
        }
    }

    private void validateFallback() {
        if (fallback == null) {
            return;
        }

        if (!"".equals(fallback.fallbackMethod())) {
            FallbackMethodCandidates candidates = FallbackMethodCandidates.create(this,
                    SpecCompatibility.createFromConfig().allowFallbackMethodExceptionParameter());
            if (candidates.isEmpty()) {
                throw fallback.fail("can't find fallback method '" + fallback.fallbackMethod()
                        + "' with matching parameter types and return type");
            }
        }
    }

    @Override
    protected List<Config> getBackoffConfigs() {
        // allows `null` elements, unlike `List.of()`
        return Arrays.asList(exponentialBackoff, fibonacciBackoff, customBackoff);
    }

    private void validateRetryWhen() {
        if (retryWhen == null) {
            return;
        }

        retryWhen.validate();

        if (retry == null) {
            throw retryWhen.fail("missing @Retry");
        }

        if (retryWhen.exception() != AlwaysOnException.class) {
            if (retry.abortOn().length != 0) {
                throw retryWhen.fail("exception", "must not be combined with @Retry.abortOn");
            }
            if (retry.retryOn().length != 1 || retry.retryOn()[0] != Exception.class) {
                throw retryWhen.fail("exception", "must not be combined with @Retry.retryOn");
            }
        }
    }

    private void validateBeforeRetry() {
        if (beforeRetry == null) {
            return;
        }

        beforeRetry.validate();

        if (retry == null) {
            throw beforeRetry.fail("missing @Retry");
        }

        if (!"".equals(beforeRetry.methodName()) && beforeRetryMethod == null) {
            throw beforeRetry.fail("methodName", "can't find before retry method '"
                    + beforeRetry.methodName() + "' with no parameter and return type of 'void'");
        }
    }

    @Override
    public void materialize() {
        super.materialize();

        if (applyFaultTolerance != null) {
            applyFaultTolerance.materialize();
        }
        if (applyGuard != null) {
            applyGuard.materialize();
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

        if (circuitBreakerName != null) {
            circuitBreakerName.materialize();
        }
        if (fallback != null) {
            fallback.materialize();
        }

        if (customBackoff != null) {
            customBackoff.materialize();
        }
        if (retryWhen != null) {
            retryWhen.materialize();
        }
        if (beforeRetry != null) {
            beforeRetry.materialize();
        }
    }

    @Override
    public String toString() {
        return "FaultToleranceOperation[" + description + "]";
    }
}
