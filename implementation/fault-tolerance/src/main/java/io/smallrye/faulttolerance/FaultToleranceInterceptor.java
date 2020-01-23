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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Priority;
import javax.enterprise.context.control.RequestContextController;
import javax.enterprise.inject.Intercepted;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

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
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.GeneralMetricsRecorder;
import io.smallrye.faulttolerance.core.Invocation;
import io.smallrye.faulttolerance.core.async.FutureExecution;
import io.smallrye.faulttolerance.core.bulkhead.CompletionStageBulkhead;
import io.smallrye.faulttolerance.core.bulkhead.SemaphoreBulkhead;
import io.smallrye.faulttolerance.core.bulkhead.ThreadPoolBulkhead;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreaker;
import io.smallrye.faulttolerance.core.circuit.breaker.CompletionStageCircuitBreaker;
import io.smallrye.faulttolerance.core.fallback.CompletionStageFallback;
import io.smallrye.faulttolerance.core.fallback.Fallback;
import io.smallrye.faulttolerance.core.fallback.FallbackFunction;
import io.smallrye.faulttolerance.core.retry.CompletionStageRetry;
import io.smallrye.faulttolerance.core.retry.Jitter;
import io.smallrye.faulttolerance.core.retry.RandomJitter;
import io.smallrye.faulttolerance.core.retry.Retry;
import io.smallrye.faulttolerance.core.retry.ThreadSleepDelay;
import io.smallrye.faulttolerance.core.stopwatch.SystemStopwatch;
import io.smallrye.faulttolerance.core.timeout.AsyncTimeout;
import io.smallrye.faulttolerance.core.timeout.CompletionStageTimeout;
import io.smallrye.faulttolerance.core.timeout.ScheduledExecutorTimeoutWatcher;
import io.smallrye.faulttolerance.core.timeout.Timeout;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;
import io.smallrye.faulttolerance.internal.InterceptionPoint;
import io.smallrye.faulttolerance.internal.RequestScopeActivator;
import io.smallrye.faulttolerance.internal.StrategyCache;
import io.smallrye.faulttolerance.metrics.MetricsCollector;
import io.smallrye.faulttolerance.metrics.MetricsCollectorFactory;

/**
 * The interceptor for fault tolerance strategies.
 *
 * @author Antoine Sabot-Durand
 * @author Martin Kouba
 * @author Michal Szynkiewicz
 */
@Interceptor
@FaultToleranceBinding
@Priority(Interceptor.Priority.PLATFORM_AFTER + 10)
public class FaultToleranceInterceptor {

    private static final Logger LOGGER = Logger.getLogger(FaultToleranceInterceptor.class);

    private final FallbackHandlerProvider fallbackHandlerProvider;

    private final Bean<?> interceptedBean;

    private final MetricsCollectorFactory metricsCollectorFactory;

    private final ScheduledExecutorService timeoutExecutor;

    private final ExecutorService asyncExecutor;

    private final FaultToleranceOperationProvider operationProvider;

    private final ExecutorProvider executorProvider;

    private final StrategyCache cache;

    private final RequestContextController requestContextController;

    @Inject
    public FaultToleranceInterceptor(
            FallbackHandlerProvider fallbackHandlerProvider,
            @Intercepted Bean<?> interceptedBean,
            MetricsCollectorFactory metricsCollectorFactory,
            FaultToleranceOperationProvider operationProvider,
            StrategyCache cache,
            RequestContextController requestContextController,
            ExecutorProvider executorProvider) {
        this.fallbackHandlerProvider = fallbackHandlerProvider;
        this.interceptedBean = interceptedBean;
        this.metricsCollectorFactory = metricsCollectorFactory;
        this.operationProvider = operationProvider;
        this.executorProvider = executorProvider;
        this.cache = cache;
        this.requestContextController = requestContextController;
        asyncExecutor = executorProvider.getGlobalExecutor();
        timeoutExecutor = executorProvider.getTimeoutExecutor();
    }

    @AroundInvoke
    public Object interceptCommand(InvocationContext invocationContext) throws Exception {
        Method method = invocationContext.getMethod();
        Class<?> beanClass = interceptedBean != null ? interceptedBean.getBeanClass() : method.getDeclaringClass();

        FaultToleranceOperation operation = operationProvider.get(beanClass, method);
        InterceptionPoint point = new InterceptionPoint(beanClass, invocationContext);

        MetricsCollector collector = getMetricsCollector(operation, point);

        if (operation.isAsync() && operation.returnsCompletionStage()) {
            return properAsyncFlow(operation, beanClass, invocationContext, collector, point);
        } else if (operation.isAsync()) {
            return futureFlow(operation, beanClass, invocationContext, collector, point);
        } else {
            return syncFlow(operation, beanClass, invocationContext, collector, point);
        }
    }

    private <T> CompletionStage<T> properAsyncFlow(FaultToleranceOperation operation,
            Class<?> beanClass,
            InvocationContext invocationContext,
            MetricsCollector collector,
            InterceptionPoint point) {
        FaultToleranceStrategy<CompletionStage<T>> strategy = cache.getStrategy(point,
                ignored -> prepareAsyncStrategy(operation, point, beanClass, invocationContext, collector));
        try {
            collector.invoked();
            return strategy.apply(new io.smallrye.faulttolerance.core.InvocationContext<>(() -> {
                CompletableFuture<T> result = new CompletableFuture<>();
                asyncExecutor.submit(() -> {
                    try {
                        // the requestContextController.activate/deactivate pair here is the minimum
                        // to pass TCK; for anything serious, Context Propagation is required!
                        // TODO does this code work together with Context Propagation?
                        requestContextController.activate();
                        //noinspection unchecked
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
                    } finally {
                        requestContextController.deactivate();
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

    @SuppressWarnings("unchecked")
    private <T> T syncFlow(FaultToleranceOperation operation,
            Class<?> beanClass,
            InvocationContext invocationContext,
            MetricsCollector collector,
            InterceptionPoint point) throws Exception {
        FaultToleranceStrategy<T> strategy = cache.getStrategy(point,
                ignored -> prepareSyncStrategy(operation, point, beanClass, invocationContext, collector));
        return strategy.apply(new io.smallrye.faulttolerance.core.InvocationContext<>(
                () -> (T) invocationContext.proceed()));
    }

    @SuppressWarnings("unchecked")
    private <T> Future<T> futureFlow(FaultToleranceOperation operation,
            Class<?> beanClass,
            InvocationContext invocationContext,
            MetricsCollector collector,
            InterceptionPoint point) throws Exception {
        FaultToleranceStrategy<Future<T>> strategy = cache.getStrategy(point,
                ignored -> prepareFutureStrategy(operation, point, beanClass, invocationContext, collector));
        return strategy.apply(new io.smallrye.faulttolerance.core.InvocationContext<>(
                () -> (Future<T>) invocationContext.proceed()));
    }

    private <T> FaultToleranceStrategy<CompletionStage<T>> prepareAsyncStrategy(
            FaultToleranceOperation operation,
            InterceptionPoint point,
            Class<?> beanClass, InvocationContext invocationContext, MetricsCollector collector) {
        FaultToleranceStrategy<CompletionStage<T>> result = Invocation.invocation();
        if (operation.hasBulkhead()) {
            BulkheadConfig bulkheadConfig = operation.getBulkhead();
            Integer size = bulkheadConfig.get(BulkheadConfig.VALUE);
            Integer queueSize = bulkheadConfig.get(BulkheadConfig.WAITING_TASK_QUEUE);
            result = new CompletionStageBulkhead<>(result,
                    "CompletionStage[" + point.name() + "]",
                    executorProvider.createAdHocExecutor(size), size,
                    queueSize,
                    collector);
        }

        if (operation.hasTimeout()) {
            long timeoutMs = getTimeInMs(operation.getTimeout(), TimeoutConfig.VALUE, TimeoutConfig.UNIT);
            result = new CompletionStageTimeout<>(result, "Timeout[" + point.name() + "]",
                    timeoutMs,
                    new ScheduledExecutorTimeoutWatcher(timeoutExecutor),
                    asyncExecutor,
                    collector);
        }

        if (operation.hasCircuitBreaker()) {
            CircuitBreakerConfig cbConfig = operation.getCircuitBreaker();
            result = new CompletionStageCircuitBreaker<>(result, "CircuitBreaker[" + point.name() + "]",
                    getSetOfThrowables(cbConfig, CircuitBreakerConfig.FAIL_ON),
                    getSetOfThrowables(cbConfig, CircuitBreakerConfig.SKIP_ON),
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
                    getSetOfThrowables(retryConf, RetryConfig.RETRY_ON),
                    getSetOfThrowables(retryConf, RetryConfig.ABORT_ON),
                    (int) retryConf.get(RetryConfig.MAX_RETRIES),
                    maxDurationMs,
                    new ThreadSleepDelay(delayMs, jitter),
                    new SystemStopwatch(),
                    collector);
        }

        if (operation.hasFallback()) {
            FallbackConfig fallbackConf = operation.getFallback();
            Method method = invocationContext.getMethod();
            result = new CompletionStageFallback<>(
                    result,
                    "Fallback[" + point.name() + "]",
                    prepareFallbackFunction(point, invocationContext, beanClass, method, operation),
                    getSetOfThrowables(fallbackConf, FallbackConfig.APPLY_ON),
                    getSetOfThrowables(fallbackConf, FallbackConfig.SKIP_ON),
                    asyncExecutor,
                    collector);
        }

        return result;
    }

    private <T> FaultToleranceStrategy<T> prepareSyncStrategy(FaultToleranceOperation operation,
            InterceptionPoint point,
            Class<?> beanClass, InvocationContext invocationContext, MetricsCollector collector) {
        FaultToleranceStrategy<T> result = Invocation.invocation();
        if (operation.hasBulkhead()) {
            BulkheadConfig bulkheadConfig = operation.getBulkhead();
            result = new SemaphoreBulkhead<>(result,
                    "Bulkhead[" + point.name() + "]",
                    bulkheadConfig.get(BulkheadConfig.VALUE),
                    collector);
        }

        if (operation.hasTimeout()) {
            long timeoutMs = getTimeInMs(operation.getTimeout(), TimeoutConfig.VALUE, TimeoutConfig.UNIT);
            result = new Timeout<>(result, "Timeout[" + point.name() + "]",
                    timeoutMs,
                    new ScheduledExecutorTimeoutWatcher(timeoutExecutor),
                    collector);
        }

        if (operation.hasCircuitBreaker()) {
            CircuitBreakerConfig cbConfig = operation.getCircuitBreaker();
            result = new CircuitBreaker<>(result, "CircuitBreaker[" + point.name() + "]",
                    getSetOfThrowables(cbConfig, CircuitBreakerConfig.FAIL_ON),
                    getSetOfThrowables(cbConfig, CircuitBreakerConfig.SKIP_ON),
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

            result = new Retry<>(result,
                    "Retry[" + point.name() + "]",
                    getSetOfThrowables(retryConf, RetryConfig.RETRY_ON),
                    getSetOfThrowables(retryConf, RetryConfig.ABORT_ON),
                    (int) retryConf.get(RetryConfig.MAX_RETRIES),
                    maxDurationMs,
                    new ThreadSleepDelay(delayMs, jitter),
                    new SystemStopwatch(),
                    collector);
        }

        if (operation.hasFallback()) {
            FallbackConfig fallbackConf = operation.getFallback();
            Method method = invocationContext.getMethod();
            result = new Fallback<>(
                    result,
                    "Fallback[" + point.name() + "]",
                    prepareFallbackFunction(point, invocationContext, beanClass, method, operation),
                    getSetOfThrowables(fallbackConf, FallbackConfig.APPLY_ON),
                    getSetOfThrowables(fallbackConf, FallbackConfig.SKIP_ON),
                    collector);
        }

        result = new GeneralMetricsRecorder<>(result, collector);

        return result;
    }

    private <T> FaultToleranceStrategy<Future<T>> prepareFutureStrategy(
            FaultToleranceOperation operation, InterceptionPoint point,
            Class<?> beanClass,
            InvocationContext invocationContext, MetricsCollector collector) {
        FaultToleranceStrategy<Future<T>> result = Invocation.invocation();

        result = new RequestScopeActivator<>(result, requestContextController);

        if (operation.hasBulkhead()) {
            BulkheadConfig bulkheadConfig = operation.getBulkhead();
            int size = bulkheadConfig.get(BulkheadConfig.VALUE);
            int queueSize = bulkheadConfig.get(BulkheadConfig.WAITING_TASK_QUEUE);
            ExecutorService executor = executorProvider.createAdHocExecutor(size);
            result = new ThreadPoolBulkhead<>(result, "Bulkhead[" + point.name() + "]",
                    executor, size, queueSize, collector);
        }

        if (operation.hasTimeout()) {
            long timeoutMs = getTimeInMs(operation.getTimeout(), TimeoutConfig.VALUE, TimeoutConfig.UNIT);
            result = new Timeout<>(result, "Timeout[" + point.name() + "]",
                    timeoutMs,
                    new ScheduledExecutorTimeoutWatcher(timeoutExecutor),
                    collector);
            result = new AsyncTimeout<>(result, asyncExecutor);
        }

        if (operation.hasCircuitBreaker()) {
            CircuitBreakerConfig cbConfig = operation.getCircuitBreaker();
            result = new CircuitBreaker<>(result, "CircuitBreaker[" + point.name() + "]",
                    getSetOfThrowables(cbConfig, CircuitBreakerConfig.FAIL_ON),
                    getSetOfThrowables(cbConfig, CircuitBreakerConfig.SKIP_ON),
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

            result = new Retry<>(result,
                    "Retry[" + point.name() + "]",
                    getSetOfThrowables(retryConf, RetryConfig.RETRY_ON),
                    getSetOfThrowables(retryConf, RetryConfig.ABORT_ON),
                    (int) retryConf.get(RetryConfig.MAX_RETRIES),
                    maxDurationMs,
                    new ThreadSleepDelay(delayMs, jitter),
                    new SystemStopwatch(),
                    collector);
        }

        if (operation.hasFallback()) {
            FallbackConfig fallbackConf = operation.getFallback();
            Method method = invocationContext.getMethod();
            FallbackFunction<Future<T>> fallbackFunction = prepareFallbackFunction(point, invocationContext, beanClass, method,
                    operation);
            result = new Fallback<>(result,
                    "Fallback[" + point.name() + "]",
                    fallbackFunction,
                    getSetOfThrowables(fallbackConf, FallbackConfig.APPLY_ON),
                    getSetOfThrowables(fallbackConf, FallbackConfig.SKIP_ON),
                    collector);
        }

        result = new GeneralMetricsRecorder<>(result, collector);

        result = new FutureExecution<>(result, asyncExecutor);

        return result;
    }

    private <V> FallbackFunction<V> prepareFallbackFunction(InterceptionPoint point, InvocationContext invocationContext,
            Class<?> beanClass, Method method, FaultToleranceOperation operation) {
        FallbackConfig fallbackConfig = operation.getFallback();
        Method fallbackMethod;
        if (!fallbackConfig.get(FallbackConfig.VALUE)
                .equals(org.eclipse.microprofile.faulttolerance.Fallback.DEFAULT.class)) {
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
        if (fallbackMethod != null) {
            fallback = whatever -> {
                try {
                    if (fallbackMethod.isDefault()) {
                        // Workaround for default methods (used e.g. in MP Rest Client)
                        //noinspection unchecked
                        return (V) DefaultMethodFallbackProvider.getFallback(fallbackMethod, executionContext);
                    } else {
                        //noinspection unchecked
                        return (V) fallbackMethod.invoke(invocationContext.getTarget(), invocationContext.getParameters());
                    }
                } catch (Throwable e) {
                    LOGGER.errorv(e, "Error determining fallback for {0}", point.name());
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
                fallback = failure -> {
                    executionContext.setFailure(failure);
                    return fallbackHandler.handle(executionContext);
                };
            } else {
                throw new IllegalStateException(
                        "Fallback defined but failed to determine the handler or method to fallback to");
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

    private List<Class<? extends Throwable>> toListOfThrowables(GenericConfig<?> config, String failOn) {
        Class<? extends Throwable>[] throwableClasses = config.get(failOn);
        return throwableClasses == null ? Collections.emptyList() : asList(throwableClasses);
    }

    private MetricsCollector getMetricsCollector(FaultToleranceOperation operation, InterceptionPoint point) {
        return cache.getMetrics(point, ignored -> metricsCollectorFactory.createCollector(operation));
    }

}
