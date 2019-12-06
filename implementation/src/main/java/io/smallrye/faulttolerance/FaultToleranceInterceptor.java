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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PrivilegedActionException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
import org.jboss.logging.Logger;

import io.smallrye.faulttolerance.config.BulkheadConfig;
import io.smallrye.faulttolerance.config.CircuitBreakerConfig;
import io.smallrye.faulttolerance.config.FallbackConfig;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;
import io.smallrye.faulttolerance.config.GenericConfig;
import io.smallrye.faulttolerance.config.RetryConfig;
import io.smallrye.faulttolerance.config.TimeoutConfig;
import io.smallrye.faulttolerance.core.Cancellator;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.FutureInvocationContext;
import io.smallrye.faulttolerance.core.Invocation;
import io.smallrye.faulttolerance.core.SimpleInvocationContext;
import io.smallrye.faulttolerance.core.bulkhead.FutureBulkhead;
import io.smallrye.faulttolerance.core.bulkhead.SyncBulkhead;
import io.smallrye.faulttolerance.core.circuit.breaker.CompletionStageCircuitBreaker;
import io.smallrye.faulttolerance.core.circuit.breaker.FutureCircuitBreaker;
import io.smallrye.faulttolerance.core.circuit.breaker.SyncCircuitBreaker;
import io.smallrye.faulttolerance.core.fallback.CompletionStageFallback;
import io.smallrye.faulttolerance.core.fallback.FallbackFunction;
import io.smallrye.faulttolerance.core.fallback.FutureFallback;
import io.smallrye.faulttolerance.core.fallback.SyncFallback;
import io.smallrye.faulttolerance.core.retry.CompletionStageRetry;
import io.smallrye.faulttolerance.core.retry.FutureRetry;
import io.smallrye.faulttolerance.core.retry.Jitter;
import io.smallrye.faulttolerance.core.retry.RandomJitter;
import io.smallrye.faulttolerance.core.retry.SyncRetry;
import io.smallrye.faulttolerance.core.retry.ThreadSleepDelay;
import io.smallrye.faulttolerance.core.stopwatch.SystemStopwatch;
import io.smallrye.faulttolerance.core.timeout.CompletionStageTimeout;
import io.smallrye.faulttolerance.core.timeout.FutureTimeout;
import io.smallrye.faulttolerance.core.timeout.ScheduledExecutorTimeoutWatcher;
import io.smallrye.faulttolerance.core.timeout.SyncTimeout;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;
import io.smallrye.faulttolerance.impl.AsyncFuture;
import io.smallrye.faulttolerance.metrics.MetricsCollector;
import io.smallrye.faulttolerance.metrics.MetricsCollectorFactory;

/**
 * mstodo: update below
 * <h2>Implementation notes:</h2>
 * <p>
 * If {@link SynchronousCircuitBreaker} is used it is not possible to track the execution inside a Hystrix command because a
 * {@link TimeoutException} should be always counted as a failure, even if the command execution completes normally.
 * </p>
 * <p>
 * We never use {@link HystrixCommand#queue()} for async execution. Mostly to workaround various problems of
 * {@link Asynchronous} {@link SyncRetry} combination. Instead, we create a composite command and inside its run() method we
 * execute commands synchronously.
 * </p>
 *
 * @author Antoine Sabot-Durand
 * @author Martin Kouba
 * @author Michal Szynkiewicz
 */
@Interceptor
@FaultToleranceBinding
@Priority(Interceptor.Priority.PLATFORM_AFTER + 10)
public class FaultToleranceInterceptor {

    /**
     * This config property key can be used to disable synchronous circuit breaker functionality. If disabled,
     * {@link SyncCircuitBreaker#successThreshold()} of value greater than 1 is not supported. Also the
     * {@link SyncCircuitBreaker#failOn()} configuration is ignored.
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

    private final Bean<?> interceptedBean;

    private final MetricsCollectorFactory metricsCollectorFactory;

    // mstodo make more flexible, figure out if that's okay!
    private final ScheduledExecutorService timeoutExecutor = Executors.newScheduledThreadPool(5);
    // mstodo modify, let customize, etc.
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(100);

    private final FaultToleranceOperationProvider operationProvider;

    @SuppressWarnings("unchecked")
    @Inject
    public FaultToleranceInterceptor(
            @ConfigProperty(name = "MP_Fault_Tolerance_NonFallback_Enabled", defaultValue = "true") Boolean nonFallBackEnable,
            Config config, FallbackHandlerProvider fallbackHandlerProvider,
            @Intercepted Bean<?> interceptedBean,
            MetricsCollectorFactory metricsCollectorFactory,
            FaultToleranceOperationProvider operationProvider) {
        this.nonFallBackEnable = nonFallBackEnable;
        this.syncCircuitBreakerEnabled = config.getOptionalValue(SYNC_CIRCUIT_BREAKER_KEY, Boolean.class).orElse(true);
        this.asyncTimeout = config.getOptionalValue(ASYNC_TIMEOUT_KEY, Boolean.class).orElse(false);
        this.fallbackHandlerProvider = fallbackHandlerProvider;
        this.interceptedBean = interceptedBean;
        this.metricsCollectorFactory = metricsCollectorFactory;
        this.operationProvider = operationProvider;
    }

    @AroundInvoke
    public Object interceptCommand(InvocationContext invocationContext) throws Exception {
        Method method = invocationContext.getMethod();
        Class<?> beanClass = interceptedBean != null ? interceptedBean.getBeanClass() : method.getDeclaringClass();

        FaultToleranceOperation operation = operationProvider.get(beanClass, method);
        InterceptionPoint point = new InterceptionPoint(beanClass, invocationContext);

        MetricsCollector collector = getMetricsCollector(operation, point);
        if (collector != null) {
            collector.invoked();
        }

        if (operation.isAsync() && operation.returnsCompletionStage()) {
            return properAsyncFlow(operation, beanClass, invocationContext, collector, point);
        } else if (operation.isAsync()) {
            Cancellator cancellator = new Cancellator();
            return offload(() -> futureFlow(operation, beanClass, invocationContext, collector, point, cancellator),
                    cancellator);
        } else {
            return syncFlow(operation, beanClass, invocationContext, collector, point);
        }
    }

    private <T> CompletionStage<T> properAsyncFlow(FaultToleranceOperation operation,
            Class<?> beanClass,
            InvocationContext invocationContext,
            MetricsCollector collector,
            InterceptionPoint point) {
        FaultToleranceStrategy<CompletionStage<T>, SimpleInvocationContext<CompletionStage<T>>> strategy = (FaultToleranceStrategy<CompletionStage<T>, SimpleInvocationContext<CompletionStage<T>>>) strategies
                .computeIfAbsent(point,
                        ignored -> prepareAsyncStrategy(operation, point, beanClass, invocationContext, collector));
        // mstodo simplify!
        try {
            return strategy.apply(new SimpleInvocationContext<>(() -> {
                CompletableFuture<T> result = new CompletableFuture<>();
                asyncExecutor.submit(() -> {
                    try {
                        ((CompletionStage<T>) invocationContext.proceed())
                                .handle((value, error) -> {
                                    if (error != null) {
                                        result.completeExceptionally(error);
                                    } else {
                                        result.complete(value);
                                    }
                                    return null;
                                });
                    } catch (Exception any) {
                        result.completeExceptionally(any);
                    }
                });
                return result;
            })).exceptionally(e -> {
                collector.failed();
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new FaultToleranceException(e);
                }
            });
        } catch (Exception e) {
            collector.failed();
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new FaultToleranceException(e);
            }
        }
    }

    private <T> Future<T> offload(Callable<T> o, Cancellator cancellator) {
        Future<Future<T>> result = (Future<Future<T>>) asyncExecutor.submit(o);
        return new AsyncFuture(result, cancellator);
    }

    @SuppressWarnings("unchecked")
    private <T> T syncFlow(FaultToleranceOperation operation,
            Class<?> beanClass,
            InvocationContext invocationContext,
            MetricsCollector collector,
            InterceptionPoint point) throws Exception {
        FaultToleranceStrategy<T, SimpleInvocationContext<T>> strategy = (FaultToleranceStrategy<T, SimpleInvocationContext<T>>) strategies
                .computeIfAbsent(point,
                        ignored -> prepareSyncStrategy(operation, point, beanClass, invocationContext, collector));
        try {
            return strategy.apply(new SimpleInvocationContext<>(() -> (T) invocationContext.proceed()));
        } catch (Exception any) {
            collector.failed();
            throw any;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Future<T> futureFlow(FaultToleranceOperation operation,
            Class<?> beanClass,
            InvocationContext invocationContext,
            MetricsCollector collector,
            InterceptionPoint point,
            Cancellator cancellator) throws Exception {
        FaultToleranceStrategy<Future<T>, FutureInvocationContext<T>> strategy = (FaultToleranceStrategy<Future<T>, FutureInvocationContext<T>>) strategies
                .computeIfAbsent(point,
                        ignored -> prepareFutureStrategy(operation, point, beanClass, invocationContext, collector));
        try {
            return strategy.apply(new FutureInvocationContext<T>(cancellator, () -> (Future<T>) invocationContext.proceed()));
        } catch (Exception any) {
            collector.failed();
            throw any;
        }
    }

    private <T> FaultToleranceStrategy<CompletionStage<T>, SimpleInvocationContext<CompletionStage<T>>> prepareAsyncStrategy(
            FaultToleranceOperation operation,
            InterceptionPoint point,
            Class<?> beanClass, InvocationContext invocationContext, MetricsCollector collector) {
        FaultToleranceStrategy<CompletionStage<T>, SimpleInvocationContext<CompletionStage<T>>> result = Invocation
                .invocation();
        if (operation.hasBulkhead()) {
            BulkheadConfig bulkheadConfig = operation.getBulkhead();
            throw new RuntimeException("Completion stage bulkhead not supported yet");
        }

        if (operation.hasTimeout()) {
            long timeoutMs = getTimeInMs(operation.getTimeout(), TimeoutConfig.VALUE, TimeoutConfig.UNIT);
            result = new CompletionStageTimeout<>(result, "Timeout[" + point.name() + "]",
                    timeoutMs,
                    new ScheduledExecutorTimeoutWatcher(timeoutExecutor),
                    asyncExecutor, // mstodo make it configurable!
                    collector);
        }

        if (operation.hasCircuitBreaker()) {
            CircuitBreakerConfig cbConfig = operation.getCircuitBreaker();
            result = new CompletionStageCircuitBreaker<>(result, "CircuitBreaker[" + point.name() + "]",
                    getSetOfThrowables(cbConfig, CircuitBreakerConfig.FAIL_ON),
                    cbConfig.get(CircuitBreakerConfig.DELAY),
                    cbConfig.get(CircuitBreakerConfig.REQUEST_VOLUME_THRESHOLD),
                    cbConfig.get(CircuitBreakerConfig.FAILURE_RATIO),
                    cbConfig.get(CircuitBreakerConfig.SUCCESS_THRESHOLD),
                    new SystemStopwatch(),
                    collector);
        }

        if (operation.hasRetry()) {
            RetryConfig retryConf = operation.getRetry();
            long maxDurationMs = getTimeInMs(retryConf, RetryConfig.MAX_DURATION, RetryConfig.DURATION_UNIT);

            long delayMs = getTimeInMs(retryConf, RetryConfig.DELAY, RetryConfig.DELAY_UNIT);

            long jitterMs = getTimeInMs(retryConf, RetryConfig.JITTER, RetryConfig.JITTER_DELAY_UNIT);
            Jitter jitter = jitterMs == 0 ? Jitter.ZERO : new RandomJitter(jitterMs);

            result = new CompletionStageRetry<>(result,
                    "Retry[" + point.name() + "]",
                    getSetOfThrowablesForRetry(retryConf, RetryConfig.RETRY_ON),
                    getSetOfThrowablesForRetry(retryConf, RetryConfig.ABORT_ON),
                    (int) retryConf.get(RetryConfig.MAX_RETRIES),
                    maxDurationMs,
                    new ThreadSleepDelay(delayMs, jitter),
                    new SystemStopwatch(),
                    collector);
        }

        if (operation.hasFallback()) {
            Method method = invocationContext.getMethod();
            result = new CompletionStageFallback<>(
                    result,
                    "Fallback[" + point.name() + "]",
                    prepareFallbackFunction(invocationContext, beanClass, method, operation),
                    asyncExecutor,
                    collector);
        }
        return result;
    }

    private <T> FaultToleranceStrategy<T, SimpleInvocationContext<T>> prepareSyncStrategy(FaultToleranceOperation operation,
            InterceptionPoint point,
            Class<?> beanClass, InvocationContext invocationContext, MetricsCollector collector) {
        FaultToleranceStrategy<T, SimpleInvocationContext<T>> result = Invocation.invocation();
        if (operation.hasBulkhead()) {
            BulkheadConfig bulkheadConfig = operation.getBulkhead();
            result = new SyncBulkhead<>(result,
                    "Bulkhead[" + point.name() + "]",
                    bulkheadConfig.get(BulkheadConfig.VALUE),
                    collector);
        }

        if (operation.hasTimeout()) {
            long timeoutMs = getTimeInMs(operation.getTimeout(), TimeoutConfig.VALUE, TimeoutConfig.UNIT);
            result = new SyncTimeout<>(result, "Timeout[" + point.name() + "]",
                    timeoutMs,
                    new ScheduledExecutorTimeoutWatcher(timeoutExecutor),
                    collector);
        }

        if (operation.hasCircuitBreaker()) {
            CircuitBreakerConfig cbConfig = operation.getCircuitBreaker();
            result = new SyncCircuitBreaker<>(result, "CircuitBreaker[" + point.name() + "]",
                    getSetOfThrowables(cbConfig, CircuitBreakerConfig.FAIL_ON),
                    cbConfig.get(CircuitBreakerConfig.DELAY),
                    cbConfig.get(CircuitBreakerConfig.REQUEST_VOLUME_THRESHOLD),
                    cbConfig.get(CircuitBreakerConfig.FAILURE_RATIO),
                    cbConfig.get(CircuitBreakerConfig.SUCCESS_THRESHOLD),
                    new SystemStopwatch(),
                    collector);
        }

        if (operation.hasRetry()) {
            RetryConfig retryConf = operation.getRetry();
            long maxDurationMs = getTimeInMs(retryConf, RetryConfig.MAX_DURATION, RetryConfig.DURATION_UNIT);

            long delayMs = getTimeInMs(retryConf, RetryConfig.DELAY, RetryConfig.DELAY_UNIT);

            long jitterMs = getTimeInMs(retryConf, RetryConfig.JITTER, RetryConfig.JITTER_DELAY_UNIT);
            Jitter jitter = jitterMs == 0 ? Jitter.ZERO : new RandomJitter(jitterMs);

            result = new SyncRetry<>(result,
                    "Retry[" + point.name() + "]",
                    getSetOfThrowablesForRetry(retryConf, RetryConfig.RETRY_ON),
                    getSetOfThrowablesForRetry(retryConf, RetryConfig.ABORT_ON),
                    (int) retryConf.get(RetryConfig.MAX_RETRIES),
                    maxDurationMs,
                    new ThreadSleepDelay(delayMs, jitter),
                    new SystemStopwatch(),
                    collector);
        }

        if (operation.hasFallback()) {
            Method method = invocationContext.getMethod();
            result = new SyncFallback<>(
                    result,
                    "Fallback[" + point.name() + "]",
                    prepareFallbackFunction(invocationContext, beanClass, method, operation),
                    collector);
        }
        return result;
    }

    private <T> FaultToleranceStrategy<Future<T>, FutureInvocationContext<T>> prepareFutureStrategy(
            FaultToleranceOperation operation, InterceptionPoint point,
            Class<?> beanClass,
            InvocationContext invocationContext, MetricsCollector collector) {
        FaultToleranceStrategy<Future<T>, FutureInvocationContext<T>> result = Invocation.invocation();
        if (operation.hasBulkhead()) {
            BulkheadConfig bulkheadConfig = operation.getBulkhead();
            result = new FutureBulkhead<>(result,
                    "Bulkhead[" + point.name() + "]",
                    bulkheadConfig.get(BulkheadConfig.VALUE),
                    bulkheadConfig.get(BulkheadConfig.WAITING_TASK_QUEUE),
                    collector);
        }

        if (operation.hasTimeout()) {
            long timeoutMs = getTimeInMs(operation.getTimeout(), TimeoutConfig.VALUE, TimeoutConfig.UNIT);
            result = new FutureTimeout<>(result, "Timeout[" + point.name() + "]",
                    timeoutMs,
                    new ScheduledExecutorTimeoutWatcher(timeoutExecutor),
                    collector,
                    asyncExecutor);
        }

        if (operation.hasCircuitBreaker()) {
            CircuitBreakerConfig cbConfig = operation.getCircuitBreaker();
            result = new FutureCircuitBreaker<>(result, "CircuitBreaker[" + point.name() + "]",
                    getSetOfThrowables(cbConfig, CircuitBreakerConfig.FAIL_ON),
                    cbConfig.get(CircuitBreakerConfig.DELAY),
                    cbConfig.get(CircuitBreakerConfig.REQUEST_VOLUME_THRESHOLD),
                    cbConfig.get(CircuitBreakerConfig.FAILURE_RATIO),
                    cbConfig.get(CircuitBreakerConfig.SUCCESS_THRESHOLD),
                    new SystemStopwatch(),
                    collector);
        }

        if (operation.hasRetry()) {
            RetryConfig retryConf = operation.getRetry();
            long maxDurationMs = getTimeInMs(retryConf, RetryConfig.MAX_DURATION, RetryConfig.DURATION_UNIT);

            long delayMs = getTimeInMs(retryConf, RetryConfig.DELAY, RetryConfig.DELAY_UNIT);

            long jitterMs = getTimeInMs(retryConf, RetryConfig.JITTER, RetryConfig.JITTER_DELAY_UNIT);
            Jitter jitter = jitterMs == 0 ? Jitter.ZERO : new RandomJitter(jitterMs);

            result = new FutureRetry<>(result,
                    "Retry[" + point.name() + "]",
                    getSetOfThrowablesForRetry(retryConf, RetryConfig.RETRY_ON),
                    getSetOfThrowablesForRetry(retryConf, RetryConfig.ABORT_ON),
                    (int) retryConf.get(RetryConfig.MAX_RETRIES),
                    maxDurationMs,
                    new ThreadSleepDelay(delayMs, jitter),
                    new SystemStopwatch(),
                    collector);
        }

        if (operation.hasFallback()) {
            Method method = invocationContext.getMethod();
            result = new FutureFallback<>(
                    result,
                    "Fallback[" + point.name() + "]",
                    prepareFallbackFunction(invocationContext, beanClass, method, operation),
                    collector);
        }
        return result;
    }

    private <V> FallbackFunction<V> prepareFallbackFunction(
            InvocationContext invocationContext,
            Class<?> beanClass,
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
        FallbackFunction<V> fallback;
        // mstodo throw an exception instead of returning null ?
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
                    // mstodo log failure?
                    if (e instanceof InvocationTargetException) {
                        e = e.getCause();
                    }
                    if (e instanceof Exception) {
                        throw (Exception) e;
                    }
                    throw new FaultToleranceException("Error during fallback method invocation", e);
                }
            };
        } else {
            FallbackHandler<V> fallbackHandler = fallbackHandlerProvider.get(operation);
            if (fallbackHandler != null) {
                fallback = new FallbackFunction<V>() {
                    @Override
                    public V call(Throwable failure) throws Exception {
                        executionContext.setFailure(failure);
                        return fallbackHandler.handle(executionContext);
                    }
                };
            } else {
                // mstodo error message
                throw new IllegalStateException("Fallback defined but failed to determine the handler or method");
            }
        }

        return fallback;
    }

    private long getTimeInMs(GenericConfig<?> config, String configKey, String unitConfigKey) {
        long time = config.get(configKey);
        ChronoUnit unit = config.get(unitConfigKey);
        return Duration.of(time, unit).toMillis();
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

    private MetricsCollector getMetricsCollector(FaultToleranceOperation operation,
            InterceptionPoint point) { // mstodo clean this up
        return metricsCollectors
                .computeIfAbsent(point,
                        ignored -> Optional.ofNullable(metricsCollectorFactory.createCollector(operation)))
                .orElse(null);
    }

    // mstodo with this we have bean-scoped FT strategies, they should probably be global
    // mstodo the problem is fallback
    private final Map<InterceptionPoint, FaultToleranceStrategy<?, ?>> strategies = new ConcurrentHashMap<>();
    private final Map<InterceptionPoint, Optional<MetricsCollector>> metricsCollectors = new ConcurrentHashMap<>();

    private static class InterceptionPoint { // mstodo pull out?
        private final String name;
        private final Class<?> beanClass;
        private final Method method;

        InterceptionPoint(Class<?> beanClass, InvocationContext invocationContext) {
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
            InterceptionPoint that = (InterceptionPoint) o;
            return beanClass.equals(that.beanClass) &&
                    method.equals(that.method);
        }

        @Override
        public int hashCode() {
            return Objects.hash(beanClass, method);
        }
    }

}
