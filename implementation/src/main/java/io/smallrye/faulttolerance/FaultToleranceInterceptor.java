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

package io.smallrye.faulttolerance;

import static java.util.Arrays.asList;

import java.lang.reflect.Method;
import java.security.PrivilegedActionException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Priority;
import javax.enterprise.inject.Intercepted;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.jboss.logging.Logger;

import com.github.ladicek.oaken_ocean.core.bulkhead.Bulkhead;
import com.github.ladicek.oaken_ocean.core.circuit.breaker.CircuitBreaker;
import com.github.ladicek.oaken_ocean.core.fallback.Fallback;
import com.github.ladicek.oaken_ocean.core.fallback.FallbackFunction;
import com.github.ladicek.oaken_ocean.core.retry.Jitter;
import com.github.ladicek.oaken_ocean.core.retry.RandomJitter;
import com.github.ladicek.oaken_ocean.core.retry.Retry;
import com.github.ladicek.oaken_ocean.core.retry.ThreadSleepDelay;
import com.github.ladicek.oaken_ocean.core.stopwatch.SystemStopwatch;
import com.github.ladicek.oaken_ocean.core.timeout.ScheduledExecutorTimeoutWatcher;
import com.github.ladicek.oaken_ocean.core.timeout.Timeout;
import com.github.ladicek.oaken_ocean.core.util.SetOfThrowables;

import io.smallrye.faulttolerance.config.BulkheadConfig;
import io.smallrye.faulttolerance.config.CircuitBreakerConfig;
import io.smallrye.faulttolerance.config.FallbackConfig;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;
import io.smallrye.faulttolerance.config.GenericConfig;
import io.smallrye.faulttolerance.config.RetryConfig;
import io.smallrye.faulttolerance.config.TimeoutConfig;
import io.smallrye.faulttolerance.metrics.MetricsCollectorFactory;

/**
 * <h2>Implementation notes:</h2>
 * <p>
 * If {@link SynchronousCircuitBreaker} is used it is not possible to track the execution inside a Hystrix command because a
 * {@link TimeoutException} should be always counted as a failure, even if the command execution completes normally.
 * </p>
 * <p>
 * We never use {@link HystrixCommand#queue()} for async execution. Mostly to workaround various problems of
 * {@link Asynchronous} {@link Retry} combination. Instead, we create a composite command and inside its run() method we
 * execute commands synchronously.
 * </p>
 *
 * @author Antoine Sabot-Durand
 * @author Martin Kouba
 */
@Interceptor
@HystrixCommandBinding
@Priority(Interceptor.Priority.PLATFORM_AFTER + 10)
public class FaultToleranceInterceptor {

    /**
     * This config property key can be used to disable synchronous circuit breaker functionality. If disabled,
     * {@link CircuitBreaker#successThreshold()} of value greater than 1 is not supported. Also the
     * {@link CircuitBreaker#failOn()} configuration is ignored.
     * <p>
     * Moreover, circuit breaker does not necessarily transition from CLOSED to OPEN immediately when a fault tolerance
     * operation completes. See also
     * <a href="https://github.com/Netflix/Hystrix/wiki/Configuration#metrics.healthSnapshot.intervalInMilliseconds">Hystrix
     * configuration</a>
     * </p>
     * <p>
     * In general, application developers are encouraged to disable this feature on high-volume circuits and in production
     * environments.
     * </p>
     */
    public static final String SYNC_CIRCUIT_BREAKER_KEY = "io_smallrye_faulttolerance_syncCircuitBreaker";

    /**
     * This config property can be used to enable timeouts for async actions (that is, {@link CompositeCommand} and
     * can be used to make sure tests don't hang. When enabled, you should also explicitly configure the timeout using
     * <a href="https://github.com/Netflix/Hystrix/wiki/Configuration#execution.isolation.thread.timeoutInMilliseconds">Hystrix
     * configuration</a>.
     */
    public static final String ASYNC_TIMEOUT_KEY = "io_smallrye_faulttolerance_asyncTimeout";

    private static final Logger LOGGER = Logger.getLogger(FaultToleranceInterceptor.class);

    private final Boolean nonFallBackEnable;

    private final Boolean syncCircuitBreakerEnabled;

    private final boolean asyncTimeout;

    private final FallbackHandlerProvider fallbackHandlerProvider;

    private final FaultToleranceOperationProvider faultToleranceOperationProvider;

    private final CommandListenersProvider listenersProvider;

    private final Bean<?> interceptedBean;

    private final MetricsCollectorFactory metricsCollectorFactory;

    // mstodo make more flexible, figure out if that's okay!
    private final ScheduledExecutorService timeoutExecutor = Executors.newScheduledThreadPool(5);

    @SuppressWarnings("unchecked")
    @Inject
    public FaultToleranceInterceptor(
            @ConfigProperty(name = "MP_Fault_Tolerance_NonFallback_Enabled", defaultValue = "true") Boolean nonFallBackEnable,
            Config config, FallbackHandlerProvider fallbackHandlerProvider,
            FaultToleranceOperationProvider faultToleranceOperationProvider,
            CommandListenersProvider listenersProvider, @Intercepted Bean<?> interceptedBean,
            MetricsCollectorFactory metricsCollectorFactory) {
        this.nonFallBackEnable = nonFallBackEnable;
        this.syncCircuitBreakerEnabled = config.getOptionalValue(SYNC_CIRCUIT_BREAKER_KEY, Boolean.class).orElse(true);
        this.asyncTimeout = config.getOptionalValue(ASYNC_TIMEOUT_KEY, Boolean.class).orElse(false);
        this.fallbackHandlerProvider = fallbackHandlerProvider;
        this.faultToleranceOperationProvider = faultToleranceOperationProvider;
        this.listenersProvider = listenersProvider;
        this.interceptedBean = interceptedBean;
        this.metricsCollectorFactory = metricsCollectorFactory;
    }

    @AroundInvoke
    public Object interceptCommand(InvocationContext invocationContext) throws Exception {

        Method method = invocationContext.getMethod();
        Class<?> beanClass = interceptedBean != null ? interceptedBean.getBeanClass() : method.getDeclaringClass();

        FaultToleranceOperation operation = FaultToleranceOperation.of(beanClass, method);

        if (operation.isAsync() && operation.returnsCompletionStage()) {
            return properAsyncFlow(invocationContext, operation);
        } else if (operation.isAsync()) {
            // mstodo interruptions, etc?
            // mstodo maybe pass continuation
            return offload(() -> syncFlow(operation, beanClass, invocationContext));
        } else {
            return syncFlow(operation, beanClass, invocationContext);
        }
    }

    private Object properAsyncFlow(InvocationContext invocationContext, FaultToleranceOperation operation) {
        throw new RuntimeException("unimplemented");
        //        try {
        //            return offload(() -> (CompletionStage<?>) syncFlow(operation, invocationContext))
        //                  .exceptionally(countfailures_onemoretimeorgiveup);
        //        } catch (Exception any) {
        ////             trigger whatever
        //            return null; // mstodo throw!
        //        }
    }

    private <T> T offload(Callable<T> o) {
        return null;
    }

    private <T> T syncFlow(FaultToleranceOperation operation,
            Class<?> beanClass,
            InvocationContext invocationContext) throws Exception {

        InterceptorPoint point = new InterceptorPoint(beanClass, invocationContext);

        Callable<T> call = () -> (T) invocationContext.proceed();

        if (operation.hasBulkhead()) {
            // mstodo bulkhead, etc instances should be shared between inocations!
            call = getBulkhead(point, operation.getBulkhead()).callable(call); //new Bulkhead(operation.getBulkhead(), call);
        }

        if (operation.hasCircuitBreaker()) {
            call = getCircuitBreaker(point, operation.getCircuitBreaker()).callable(call);
        }

        if (operation.hasTimeout()) {
            long timeoutMs = getTimeInMs(operation.getTimeout(), TimeoutConfig.VALUE, TimeoutConfig.UNIT);
            call = new Timeout<>(
                    call,
                    "Timeout[" + point.name() + "]",
                    timeoutMs,
                    new ScheduledExecutorTimeoutWatcher(timeoutExecutor));
        }

        if (operation.hasRetry()) {
            RetryConfig retryConf = operation.getRetry();
            long maxDurationMs = getTimeInMs(retryConf, RetryConfig.MAX_DURATION, RetryConfig.DURATION_UNIT);

            long delayMs = getTimeInMs(retryConf, RetryConfig.DELAY, RetryConfig.DELAY_UNIT);

            long jitterMs = getTimeInMs(retryConf, RetryConfig.JITTER, RetryConfig.JITTER_DELAY_UNIT);
            Jitter jitter = new RandomJitter(jitterMs);

            call = new Retry<T>(
                    call,
                    "Retry[" + point.name() + "]",
                    getSetOfThrowablesForRetry(retryConf, RetryConfig.RETRY_ON),
                    getSetOfThrowablesForRetry(retryConf, RetryConfig.ABORT_ON),
                    (int) retryConf.get(RetryConfig.MAX_RETRIES),
                    maxDurationMs,
                    new ThreadSleepDelay(delayMs, jitter),
                    new SystemStopwatch());
        }

        if (operation.hasFallback()) {
            Method method = invocationContext.getMethod();
            call = new Fallback<>(
                    call,
                    "Fallback[" + point.name() + "]",
                    prepareFallbackFunction(invocationContext, beanClass, method, operation));
        }

        return call.call();
    }

    private <V> FallbackFunction<V> prepareFallbackFunction(
            InvocationContext invocationContext,
            Class beanClass,
            Method method,
            FaultToleranceOperation operation) {
        FallbackConfig fallbackConfig = operation.getFallback();
        Method fallbackMethod;
        if (!fallbackConfig.get(FallbackConfig.VALUE).equals(org.eclipse.microprofile.faulttolerance.Fallback.DEFAULT.class)) {
            fallbackMethod = null;
        } else {
            String fallbackMethodName = fallbackConfig.get(FallbackConfig.FALLBACK_METHOD);
            if (!"".equals(fallbackMethodName)) {
                try {
                    fallbackMethod = SecurityActions.getDeclaredMethod(beanClass, method.getDeclaringClass(),
                            fallbackMethodName, method.getGenericParameterTypes());
                    if (fallbackMethod == null) {
                        throw new FaultToleranceException("Could not obtain fallback method " + fallbackMethodName);
                    }
                    SecurityActions.setAccessible(fallbackMethod);
                } catch (PrivilegedActionException e) {
                    throw new FaultToleranceException("Could not obtain fallback method", e);
                }
            } else {
                fallbackMethod = null;
            }
        }

        ExecutionContextWithInvocationContext executionContext = new ExecutionContextWithInvocationContext(invocationContext);
        FallbackFunction<V> fallback = null;
        // mstodo throw an exception instaed of returning null ?
        if (fallbackMethod != null) {
            fallback = whatever -> {
                try {
                    if (fallbackMethod.isDefault()) {
                        // Workaround for default methods (used e.g. in MP Rest Client)
                        return (V) DefaultMethodFallbackProvider.getFallback(fallbackMethod, executionContext);
                    } else {
                        return (V) fallbackMethod.invoke(invocationContext.getTarget(), invocationContext.getParameters());
                    }
                } catch (Throwable e) {
                    throw new FaultToleranceException("Error during fallback method invocation", e);
                }
            };
        } else {
            FallbackHandler<V> fallbackHandler = fallbackHandlerProvider.get(operation);
            if (fallbackHandler != null) {
                fallback = whatever -> fallbackHandler.handle(executionContext);
            }
        }

        return fallback;
    }

    private long getTimeInMs(GenericConfig<?> config, String configKey, String unitConfigKey) {
        long time = config.get(configKey);
        ChronoUnit unit = config.get(unitConfigKey);
        return Duration.of(time, unit).toMillis();
    }

    private CircuitBreaker getCircuitBreaker(InterceptorPoint point, CircuitBreakerConfig config) {
        return circuitBreakers.computeIfAbsent(point,
                anything -> {
                    String failOn = CircuitBreakerConfig.FAIL_ON;
                    return new CircuitBreaker(
                            "CircuitBreaker[" + point.name() + "]",
                            getSetOfThrowables(config, failOn),
                            config.get(CircuitBreakerConfig.DELAY),
                            config.get(CircuitBreakerConfig.REQUEST_VOLUME_THRESHOLD),
                            config.get(CircuitBreakerConfig.FAILURE_RATIO),
                            config.get(CircuitBreakerConfig.SUCCESS_THRESHOLD),
                            new SystemStopwatch());
                });
    }

    private SetOfThrowables getSetOfThrowables(GenericConfig<?> config, String configKey) {
        List<Class<? extends Throwable>> throwableClassList = toListOfThrowables(config, configKey);
        return SetOfThrowables.create(throwableClassList);
    }

    private SetOfThrowables getSetOfThrowablesForRetry(GenericConfig<?> config, String configKey) {
        List<Class<? extends Throwable>> throwableClassList = toListOfThrowables(config, configKey);
        return SetOfThrowables.create(throwableClassList);
    }

    private List<Class<? extends Throwable>> toListOfThrowables(GenericConfig<?> config, String failOn) {
        Class<? extends Throwable>[] throwableClasses = config.get(failOn);
        return throwableClasses == null ? Collections.emptyList() : asList(throwableClasses);
    }

    private Bulkhead getBulkhead(InterceptorPoint point, BulkheadConfig bulkheadConfig) {
        return bulkheads.computeIfAbsent(point,
                ignored -> new Bulkhead(bulkheadConfig.get(BulkheadConfig.VALUE)));
    }

    private final Map<InterceptorPoint, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    private final Map<InterceptorPoint, Bulkhead> bulkheads = new ConcurrentHashMap<>();

    private static class InterceptorPoint { // mstodo pull out!
        private final String name;
        private final Class<?> beanClass;
        private final Method method;

        public InterceptorPoint(Class<?> beanClass, InvocationContext invocationContext) {
            this.beanClass = beanClass;
            method = invocationContext.getMethod();
            name = beanClass.getName() + "#" + method.getName();
        }

        public String name() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            InterceptorPoint that = (InterceptorPoint) o;
            return beanClass.equals(that.beanClass) &&
                    method.equals(that.method);
        }

        @Override
        public int hashCode() {
            return Objects.hash(beanClass, method);
        }
    }

}
