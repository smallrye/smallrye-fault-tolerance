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

import static io.smallrye.faulttolerance.core.util.CompletionStages.failedStage;
import static io.smallrye.faulttolerance.core.util.CompletionStages.propagateCompletion;
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
import javax.enterprise.event.Event;
import javax.enterprise.inject.Intercepted;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import io.smallrye.faulttolerance.api.CircuitBreakerStateChanged;
import io.smallrye.faulttolerance.config.BulkheadConfig;
import io.smallrye.faulttolerance.config.CircuitBreakerConfig;
import io.smallrye.faulttolerance.config.FallbackConfig;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;
import io.smallrye.faulttolerance.config.GenericConfig;
import io.smallrye.faulttolerance.config.RetryConfig;
import io.smallrye.faulttolerance.config.TimeoutConfig;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
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
import io.smallrye.faulttolerance.core.metrics.CompletionStageMetricsCollector;
import io.smallrye.faulttolerance.core.metrics.MetricsCollector;
import io.smallrye.faulttolerance.core.metrics.MetricsRecorder;
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
import io.smallrye.faulttolerance.internal.CircuitBreakerStateObserver;
import io.smallrye.faulttolerance.internal.InterceptionPoint;
import io.smallrye.faulttolerance.internal.RequestContextControllerProvider;
import io.smallrye.faulttolerance.internal.RequestScopeActivator;
import io.smallrye.faulttolerance.internal.StrategyCache;
import io.smallrye.faulttolerance.metrics.MetricsProvider;

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

    private final FallbackHandlerProvider fallbackHandlerProvider;

    private final Bean<?> interceptedBean;

    private final MetricsProvider metricsProvider;

    private final ScheduledExecutorService timeoutExecutor;

    private final ExecutorService asyncExecutor;

    private final FaultToleranceOperationProvider operationProvider;

    private final ExecutorProvider executorProvider;

    private final StrategyCache cache;

    private final RequestContextController requestContextController;

    private final Event<CircuitBreakerStateChanged> cbStateEvent;

    @Inject
    public FaultToleranceInterceptor(
            FallbackHandlerProvider fallbackHandlerProvider,
            @Intercepted Bean<?> interceptedBean,
            MetricsProvider metricsProvider,
            FaultToleranceOperationProvider operationProvider,
            StrategyCache cache,
            ExecutorProvider executorProvider,
            Event<CircuitBreakerStateChanged> cbStateEvent) {
        this.fallbackHandlerProvider = fallbackHandlerProvider;
        this.interceptedBean = interceptedBean;
        this.metricsProvider = metricsProvider;
        this.operationProvider = operationProvider;
        this.executorProvider = executorProvider;
        this.cache = cache;
        this.cbStateEvent = cbStateEvent;
        asyncExecutor = executorProvider.getGlobalExecutor();
        timeoutExecutor = executorProvider.getTimeoutExecutor();
        requestContextController = RequestContextControllerProvider.load().get();
    }

    @AroundInvoke
    public Object interceptCommand(InvocationContext invocationContext) throws Exception {
        Method method = invocationContext.getMethod();
        Class<?> beanClass = interceptedBean != null ? interceptedBean.getBeanClass() : method.getDeclaringClass();
        InterceptionPoint point = new InterceptionPoint(beanClass, invocationContext);

        FaultToleranceOperation operation = operationProvider.get(beanClass, method);

        if (operation.isAsync() && operation.returnsCompletionStage()) {
            return properAsyncFlow(operation, invocationContext, point);
        } else if (operation.isAsync()) {
            return futureFlow(operation, invocationContext, point);
        } else {
            return syncFlow(operation, invocationContext, point);
        }
    }

    private <T> CompletionStage<T> properAsyncFlow(FaultToleranceOperation operation, InvocationContext invocationContext,
            InterceptionPoint point) {
        FaultToleranceStrategy<CompletionStage<T>> strategy = cache.getStrategy(point,
                ignored -> prepareAsyncStrategy(operation, point));
        try {
            io.smallrye.faulttolerance.core.InvocationContext<CompletionStage<T>> ctx = new io.smallrye.faulttolerance.core.InvocationContext<>(
                    () -> {
                        CompletableFuture<T> result = new CompletableFuture<>();
                        asyncExecutor.submit(() -> {
                            try {
                                // the requestContextController.activate/deactivate pair here is the minimum
                                // to pass TCK; for anything serious, Context Propagation is required
                                requestContextController.activate();
                                //noinspection unchecked
                                propagateCompletion(((CompletionStage<T>) invocationContext.proceed()), result);
                            } catch (Exception any) {
                                result.completeExceptionally(any);
                            } finally {
                                requestContextController.deactivate();
                            }
                        });
                        return result;
                    });
            ctx.set(InvocationContext.class, invocationContext);
            return strategy.apply(ctx);
        } catch (Exception e) {
            return failedStage(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T syncFlow(FaultToleranceOperation operation, InvocationContext invocationContext, InterceptionPoint point)
            throws Exception {
        FaultToleranceStrategy<T> strategy = cache.getStrategy(point, ignored -> prepareSyncStrategy(operation, point));
        io.smallrye.faulttolerance.core.InvocationContext<T> ctx = new io.smallrye.faulttolerance.core.InvocationContext<>(
                () -> (T) invocationContext.proceed());
        ctx.set(InvocationContext.class, invocationContext);
        return strategy.apply(ctx);
    }

    @SuppressWarnings("unchecked")
    private <T> Future<T> futureFlow(FaultToleranceOperation operation, InvocationContext invocationContext,
            InterceptionPoint point) throws Exception {
        FaultToleranceStrategy<Future<T>> strategy = cache.getStrategy(point,
                ignored -> prepareFutureStrategy(operation, point));
        io.smallrye.faulttolerance.core.InvocationContext<Future<T>> ctx = new io.smallrye.faulttolerance.core.InvocationContext<>(
                () -> (Future<T>) invocationContext.proceed());
        ctx.set(InvocationContext.class, invocationContext);
        return strategy.apply(ctx);
    }

    private <T> FaultToleranceStrategy<CompletionStage<T>> prepareAsyncStrategy(FaultToleranceOperation operation,
            InterceptionPoint point) {
        FaultToleranceStrategy<CompletionStage<T>> result = Invocation.invocation();
        if (operation.hasBulkhead()) {
            BulkheadConfig bulkheadConfig = operation.getBulkhead();
            Integer size = bulkheadConfig.get(BulkheadConfig.VALUE);
            Integer queueSize = bulkheadConfig.get(BulkheadConfig.WAITING_TASK_QUEUE);
            result = new CompletionStageBulkhead<>(result,
                    "CompletionStage[" + point + "]",
                    executorProvider.createAdHocExecutor(size), size,
                    queueSize);
        }

        if (operation.hasTimeout()) {
            long timeoutMs = getTimeInMs(operation.getTimeout(), TimeoutConfig.VALUE, TimeoutConfig.UNIT);
            result = new CompletionStageTimeout<>(result, "Timeout[" + point + "]",
                    timeoutMs,
                    new ScheduledExecutorTimeoutWatcher(timeoutExecutor),
                    asyncExecutor);
        }

        if (operation.hasCircuitBreaker()) {
            CircuitBreakerConfig cbConfig = operation.getCircuitBreaker();
            long delayInMillis = getTimeInMs(cbConfig, CircuitBreakerConfig.DELAY, CircuitBreakerConfig.DELAY_UNIT);
            result = new CompletionStageCircuitBreaker<>(result, "CircuitBreaker[" + point + "]",
                    getSetOfThrowables(cbConfig, CircuitBreakerConfig.FAIL_ON),
                    getSetOfThrowables(cbConfig, CircuitBreakerConfig.SKIP_ON),
                    delayInMillis,
                    cbConfig.get(CircuitBreakerConfig.REQUEST_VOLUME_THRESHOLD),
                    cbConfig.get(CircuitBreakerConfig.FAILURE_RATIO),
                    cbConfig.get(CircuitBreakerConfig.SUCCESS_THRESHOLD),
                    new SystemStopwatch());
            result = new CircuitBreakerStateObserver<>(result, point, cbStateEvent);
        }

        if (operation.hasRetry()) {
            RetryConfig retryConf = operation.getRetry();
            long maxDurationMs = getTimeInMs(retryConf, RetryConfig.MAX_DURATION, RetryConfig.DURATION_UNIT);

            long delayMs = getTimeInMs(retryConf, RetryConfig.DELAY, RetryConfig.DELAY_UNIT);

            long jitterMs = getTimeInMs(retryConf, RetryConfig.JITTER, RetryConfig.JITTER_DELAY_UNIT);
            Jitter jitter = jitterMs == 0 ? Jitter.ZERO : new RandomJitter(jitterMs);

            result = new CompletionStageRetry<>(result,
                    "Retry[" + point + "]",
                    getSetOfThrowables(retryConf, RetryConfig.RETRY_ON),
                    getSetOfThrowables(retryConf, RetryConfig.ABORT_ON),
                    (int) retryConf.get(RetryConfig.MAX_RETRIES),
                    maxDurationMs,
                    new ThreadSleepDelay(delayMs, jitter),
                    new SystemStopwatch());
        }

        if (operation.hasFallback()) {
            FallbackConfig fallbackConf = operation.getFallback();
            result = new CompletionStageFallback<>(
                    result,
                    "Fallback[" + point + "]",
                    prepareFallbackFunction(point, operation),
                    getSetOfThrowables(fallbackConf, FallbackConfig.APPLY_ON),
                    getSetOfThrowables(fallbackConf, FallbackConfig.SKIP_ON));
        }

        if (metricsProvider.isEnabled()) {
            result = new CompletionStageMetricsCollector<>(result, getMetricsRecorder(operation, point));
        }

        return result;
    }

    private <T> FaultToleranceStrategy<T> prepareSyncStrategy(FaultToleranceOperation operation, InterceptionPoint point) {
        FaultToleranceStrategy<T> result = Invocation.invocation();
        if (operation.hasBulkhead()) {
            BulkheadConfig bulkheadConfig = operation.getBulkhead();
            result = new SemaphoreBulkhead<>(result,
                    "Bulkhead[" + point + "]",
                    bulkheadConfig.get(BulkheadConfig.VALUE));
        }

        if (operation.hasTimeout()) {
            long timeoutMs = getTimeInMs(operation.getTimeout(), TimeoutConfig.VALUE, TimeoutConfig.UNIT);
            result = new Timeout<>(result, "Timeout[" + point + "]",
                    timeoutMs,
                    new ScheduledExecutorTimeoutWatcher(timeoutExecutor));
        }

        if (operation.hasCircuitBreaker()) {
            CircuitBreakerConfig cbConfig = operation.getCircuitBreaker();
            long delayInMillis = getTimeInMs(cbConfig, CircuitBreakerConfig.DELAY, CircuitBreakerConfig.DELAY_UNIT);
            result = new CircuitBreaker<>(result, "CircuitBreaker[" + point + "]",
                    getSetOfThrowables(cbConfig, CircuitBreakerConfig.FAIL_ON),
                    getSetOfThrowables(cbConfig, CircuitBreakerConfig.SKIP_ON),
                    delayInMillis,
                    cbConfig.get(CircuitBreakerConfig.REQUEST_VOLUME_THRESHOLD),
                    cbConfig.get(CircuitBreakerConfig.FAILURE_RATIO),
                    cbConfig.get(CircuitBreakerConfig.SUCCESS_THRESHOLD),
                    new SystemStopwatch());
            result = new CircuitBreakerStateObserver<>(result, point, cbStateEvent);
        }

        if (operation.hasRetry()) {
            RetryConfig retryConf = operation.getRetry();
            long maxDurationMs = getTimeInMs(retryConf, RetryConfig.MAX_DURATION, RetryConfig.DURATION_UNIT);

            long delayMs = getTimeInMs(retryConf, RetryConfig.DELAY, RetryConfig.DELAY_UNIT);

            long jitterMs = getTimeInMs(retryConf, RetryConfig.JITTER, RetryConfig.JITTER_DELAY_UNIT);
            Jitter jitter = jitterMs == 0 ? Jitter.ZERO : new RandomJitter(jitterMs);

            result = new Retry<>(result,
                    "Retry[" + point + "]",
                    getSetOfThrowables(retryConf, RetryConfig.RETRY_ON),
                    getSetOfThrowables(retryConf, RetryConfig.ABORT_ON),
                    (int) retryConf.get(RetryConfig.MAX_RETRIES),
                    maxDurationMs,
                    new ThreadSleepDelay(delayMs, jitter),
                    new SystemStopwatch());
        }

        if (operation.hasFallback()) {
            FallbackConfig fallbackConf = operation.getFallback();
            result = new Fallback<>(
                    result,
                    "Fallback[" + point + "]",
                    prepareFallbackFunction(point, operation),
                    getSetOfThrowables(fallbackConf, FallbackConfig.APPLY_ON),
                    getSetOfThrowables(fallbackConf, FallbackConfig.SKIP_ON));
        }

        if (metricsProvider.isEnabled()) {
            result = new MetricsCollector<>(result, getMetricsRecorder(operation, point), false);
        }

        return result;
    }

    private <T> FaultToleranceStrategy<Future<T>> prepareFutureStrategy(FaultToleranceOperation operation,
            InterceptionPoint point) {
        FaultToleranceStrategy<Future<T>> result = Invocation.invocation();

        result = new RequestScopeActivator<>(result, requestContextController);

        if (operation.hasBulkhead()) {
            BulkheadConfig bulkheadConfig = operation.getBulkhead();
            int size = bulkheadConfig.get(BulkheadConfig.VALUE);
            int queueSize = bulkheadConfig.get(BulkheadConfig.WAITING_TASK_QUEUE);
            ExecutorService executor = executorProvider.createAdHocExecutor(size);
            result = new ThreadPoolBulkhead<>(result, "Bulkhead[" + point + "]",
                    executor, size, queueSize);
        }

        if (operation.hasTimeout()) {
            long timeoutMs = getTimeInMs(operation.getTimeout(), TimeoutConfig.VALUE, TimeoutConfig.UNIT);
            result = new Timeout<>(result, "Timeout[" + point + "]",
                    timeoutMs,
                    new ScheduledExecutorTimeoutWatcher(timeoutExecutor));
            result = new AsyncTimeout<>(result, asyncExecutor);
        }

        if (operation.hasCircuitBreaker()) {
            CircuitBreakerConfig cbConfig = operation.getCircuitBreaker();
            long delayInMillis = getTimeInMs(cbConfig, CircuitBreakerConfig.DELAY, CircuitBreakerConfig.DELAY_UNIT);
            result = new CircuitBreaker<>(result, "CircuitBreaker[" + point + "]",
                    getSetOfThrowables(cbConfig, CircuitBreakerConfig.FAIL_ON),
                    getSetOfThrowables(cbConfig, CircuitBreakerConfig.SKIP_ON),
                    delayInMillis,
                    cbConfig.get(CircuitBreakerConfig.REQUEST_VOLUME_THRESHOLD),
                    cbConfig.get(CircuitBreakerConfig.FAILURE_RATIO),
                    cbConfig.get(CircuitBreakerConfig.SUCCESS_THRESHOLD),
                    new SystemStopwatch());
            result = new CircuitBreakerStateObserver<>(result, point, cbStateEvent);
        }

        if (operation.hasRetry()) {
            RetryConfig retryConf = operation.getRetry();
            long maxDurationMs = getTimeInMs(retryConf, RetryConfig.MAX_DURATION, RetryConfig.DURATION_UNIT);

            long delayMs = getTimeInMs(retryConf, RetryConfig.DELAY, RetryConfig.DELAY_UNIT);

            long jitterMs = getTimeInMs(retryConf, RetryConfig.JITTER, RetryConfig.JITTER_DELAY_UNIT);
            Jitter jitter = jitterMs == 0 ? Jitter.ZERO : new RandomJitter(jitterMs);

            result = new Retry<>(result,
                    "Retry[" + point + "]",
                    getSetOfThrowables(retryConf, RetryConfig.RETRY_ON),
                    getSetOfThrowables(retryConf, RetryConfig.ABORT_ON),
                    (int) retryConf.get(RetryConfig.MAX_RETRIES),
                    maxDurationMs,
                    new ThreadSleepDelay(delayMs, jitter),
                    new SystemStopwatch());
        }

        if (operation.hasFallback()) {
            FallbackConfig fallbackConf = operation.getFallback();
            result = new Fallback<>(result,
                    "Fallback[" + point + "]",
                    prepareFallbackFunction(point, operation),
                    getSetOfThrowables(fallbackConf, FallbackConfig.APPLY_ON),
                    getSetOfThrowables(fallbackConf, FallbackConfig.SKIP_ON));
        }

        if (metricsProvider.isEnabled()) {
            result = new MetricsCollector<>(result, getMetricsRecorder(operation, point), true);
        }

        result = new FutureExecution<>(result, asyncExecutor);

        return result;
    }

    private <V> FallbackFunction<V> prepareFallbackFunction(InterceptionPoint point, FaultToleranceOperation operation) {
        Method fallbackMethod = null;

        FallbackConfig fallbackConfig = operation.getFallback();
        Class<? extends FallbackHandler<?>> fallback = fallbackConfig.get(FallbackConfig.VALUE);
        String fallbackMethodName = fallbackConfig.get(FallbackConfig.FALLBACK_METHOD);

        if (fallback.equals(org.eclipse.microprofile.faulttolerance.Fallback.DEFAULT.class) && !"".equals(fallbackMethodName)) {
            try {
                Method method = point.method();
                fallbackMethod = SecurityActions.getDeclaredMethod(point.beanClass(), method.getDeclaringClass(),
                        fallbackMethodName, method.getGenericParameterTypes());
                if (fallbackMethod == null) {
                    throw new FaultToleranceException("Could not obtain fallback method " + fallbackMethodName);
                }
                SecurityActions.setAccessible(fallbackMethod);
            } catch (PrivilegedActionException e) {
                throw new FaultToleranceException("Could not obtain fallback method", e);
            }
        }

        Method fallbackMethodFinal = fallbackMethod;
        if (fallbackMethod != null) {
            boolean isDefault = fallbackMethodFinal.isDefault();
            return ctx -> {
                InvocationContext interceptionContext = ctx.invocationContext.get(InvocationContext.class);
                ExecutionContextWithInvocationContext executionContext = new ExecutionContextWithInvocationContext(
                        interceptionContext);
                try {
                    if (isDefault) {
                        // Workaround for default methods (used e.g. in MP Rest Client)
                        //noinspection unchecked
                        return (V) DefaultMethodFallbackProvider.getFallback(fallbackMethodFinal, executionContext);
                    } else {
                        //noinspection unchecked
                        return (V) fallbackMethodFinal.invoke(interceptionContext.getTarget(),
                                interceptionContext.getParameters());
                    }
                } catch (Throwable e) {
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
                return ctx -> {
                    InvocationContext interceptionContext = ctx.invocationContext.get(InvocationContext.class);
                    ExecutionContextWithInvocationContext executionContext = new ExecutionContextWithInvocationContext(
                            interceptionContext);
                    executionContext.setFailure(ctx.failure);
                    return fallbackHandler.handle(executionContext);
                };
            } else {
                throw new FaultToleranceException("Could not obtain fallback handler for " + point);
            }
        }
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

    private MetricsRecorder getMetricsRecorder(FaultToleranceOperation operation, InterceptionPoint point) {
        return cache.getMetrics(point, ignored -> metricsProvider.create(operation));
    }
}
