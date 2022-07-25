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

import static io.smallrye.faulttolerance.core.Invocation.invocation;
import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PrivilegedActionException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import javax.annotation.Priority;
import javax.enterprise.context.control.RequestContextController;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Intercepted;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.CustomBackoffStrategy;
import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.apiimpl.LazyFaultTolerance;
import io.smallrye.faulttolerance.core.async.CompletionStageExecution;
import io.smallrye.faulttolerance.core.async.FutureExecution;
import io.smallrye.faulttolerance.core.async.RememberEventLoop;
import io.smallrye.faulttolerance.core.bulkhead.CompletionStageThreadPoolBulkhead;
import io.smallrye.faulttolerance.core.bulkhead.FutureThreadPoolBulkhead;
import io.smallrye.faulttolerance.core.bulkhead.SemaphoreBulkhead;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreaker;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerEvents;
import io.smallrye.faulttolerance.core.circuit.breaker.CompletionStageCircuitBreaker;
import io.smallrye.faulttolerance.core.event.loop.EventLoop;
import io.smallrye.faulttolerance.core.fallback.AsyncFallbackFunction;
import io.smallrye.faulttolerance.core.fallback.CompletionStageFallback;
import io.smallrye.faulttolerance.core.fallback.Fallback;
import io.smallrye.faulttolerance.core.fallback.FallbackFunction;
import io.smallrye.faulttolerance.core.invocation.AsyncSupport;
import io.smallrye.faulttolerance.core.invocation.AsyncSupportRegistry;
import io.smallrye.faulttolerance.core.invocation.Invoker;
import io.smallrye.faulttolerance.core.invocation.NormalMethodInvoker;
import io.smallrye.faulttolerance.core.invocation.SpecialMethodInvoker;
import io.smallrye.faulttolerance.core.invocation.StrategyInvoker;
import io.smallrye.faulttolerance.core.metrics.CompletionStageMetricsCollector;
import io.smallrye.faulttolerance.core.metrics.MetricsCollector;
import io.smallrye.faulttolerance.core.metrics.MetricsRecorder;
import io.smallrye.faulttolerance.core.rate.limit.CompletionStageRateLimit;
import io.smallrye.faulttolerance.core.rate.limit.RateLimit;
import io.smallrye.faulttolerance.core.retry.BackOff;
import io.smallrye.faulttolerance.core.retry.CompletionStageRetry;
import io.smallrye.faulttolerance.core.retry.ConstantBackOff;
import io.smallrye.faulttolerance.core.retry.CustomBackOff;
import io.smallrye.faulttolerance.core.retry.ExponentialBackOff;
import io.smallrye.faulttolerance.core.retry.FibonacciBackOff;
import io.smallrye.faulttolerance.core.retry.Jitter;
import io.smallrye.faulttolerance.core.retry.RandomJitter;
import io.smallrye.faulttolerance.core.retry.Retry;
import io.smallrye.faulttolerance.core.retry.ThreadSleepDelay;
import io.smallrye.faulttolerance.core.retry.TimerDelay;
import io.smallrye.faulttolerance.core.stopwatch.SystemStopwatch;
import io.smallrye.faulttolerance.core.timeout.AsyncTimeout;
import io.smallrye.faulttolerance.core.timeout.CompletionStageTimeout;
import io.smallrye.faulttolerance.core.timeout.Timeout;
import io.smallrye.faulttolerance.core.timeout.TimerTimeoutWatcher;
import io.smallrye.faulttolerance.core.timer.Timer;
import io.smallrye.faulttolerance.core.util.DirectExecutor;
import io.smallrye.faulttolerance.core.util.ExceptionDecision;
import io.smallrye.faulttolerance.core.util.SetBasedExceptionDecision;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;
import io.smallrye.faulttolerance.internal.InterceptionInvoker;
import io.smallrye.faulttolerance.internal.InterceptionPoint;
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

    private final Bean<?> interceptedBean;

    private final FaultToleranceOperationProvider operationProvider;

    private final StrategyCache cache;

    private final FallbackHandlerProvider fallbackHandlerProvider;

    private final MetricsProvider metricsProvider;

    private final ExecutorService asyncExecutor;

    private final EventLoop eventLoop;

    private final Timer timer;

    private final RequestContextController requestContextController;

    private final CircuitBreakerMaintenanceImpl cbMaintenance;

    private final SpecCompatibility specCompatibility;

    private final Instance<FaultTolerance<?>> configuredFaultTolerance;

    @Inject
    public FaultToleranceInterceptor(
            @Intercepted Bean<?> interceptedBean,
            FaultToleranceOperationProvider operationProvider,
            StrategyCache cache,
            FallbackHandlerProvider fallbackHandlerProvider,
            MetricsProvider metricsProvider,
            ExecutorHolder executorHolder,
            RequestContextIntegration requestContextIntegration,
            CircuitBreakerMaintenanceImpl cbMaintenance,
            SpecCompatibility specCompatibility,
            @Any Instance<FaultTolerance<?>> configuredFaultTolerance) {
        this.interceptedBean = interceptedBean;
        this.operationProvider = operationProvider;
        this.cache = cache;
        this.fallbackHandlerProvider = fallbackHandlerProvider;
        this.metricsProvider = metricsProvider;
        asyncExecutor = executorHolder.getAsyncExecutor();
        eventLoop = executorHolder.getEventLoop();
        timer = executorHolder.getTimer();
        requestContextController = requestContextIntegration.get();
        this.cbMaintenance = cbMaintenance;
        this.specCompatibility = specCompatibility;
        this.configuredFaultTolerance = configuredFaultTolerance;
    }

    @AroundInvoke
    public Object intercept(InvocationContext interceptionContext) throws Exception {
        Method method = interceptionContext.getMethod();
        Class<?> beanClass = interceptedBean != null ? interceptedBean.getBeanClass() : method.getDeclaringClass();
        InterceptionPoint point = new InterceptionPoint(beanClass, interceptionContext.getMethod());
        FaultToleranceOperation operation = operationProvider.get(beanClass, method);

        if (operation.hasApplyFaultTolerance()) {
            return preconfiguredFlow(operation, interceptionContext);
        } else if (specCompatibility.isOperationTrulyAsynchronous(operation)) {
            return asyncFlow(operation, interceptionContext, point);
        } else if (specCompatibility.isOperationPseudoAsynchronous(operation)) {
            return futureFlow(operation, interceptionContext, point);
        } else {
            return syncFlow(operation, interceptionContext, point);
        }
    }

    private Object preconfiguredFlow(FaultToleranceOperation operation, InvocationContext interceptionContext)
            throws Exception {
        String identifier = operation.getApplyFaultTolerance().value();
        Instance<FaultTolerance<?>> instance = configuredFaultTolerance.select(Identifier.Literal.of(identifier));
        if (!instance.isResolvable()) {
            throw new FaultToleranceException("Can't resolve a bean of type " + FaultTolerance.class.getName()
                    + " with qualifier @" + Identifier.class.getName() + "(\"" + identifier + "\")");
        }
        FaultTolerance<Object> faultTolerance = (FaultTolerance<Object>) instance.get();
        if (!(faultTolerance instanceof LazyFaultTolerance)) {
            throw new FaultToleranceException("Configured fault tolerance '" + identifier
                    + "' is not created by the FaultTolerance API, this is not supported");
        }

        Class<?> asyncType = ((LazyFaultTolerance<?>) faultTolerance).internalGetAsyncType();

        AsyncSupport<?, ?> forOperation = AsyncSupportRegistry.get(operation.getParameterTypes(), operation.getReturnType());
        AsyncSupport<?, ?> fromConfigured = asyncType == null ? null : AsyncSupportRegistry.get(new Class[0], asyncType);

        if (forOperation == null && fromConfigured == null) {
            return faultTolerance.call(interceptionContext::proceed);
        } else if (forOperation == null) {
            throw new FaultToleranceException("Configured fault tolerance '" + identifier
                    + "' expects the operation to " + fromConfigured.mustDescription()
                    + ", but the operation is synchronous: " + operation);
        } else if (fromConfigured == null) {
            throw new FaultToleranceException("Configured fault tolerance '" + identifier
                    + "' expects the operation to be synchronous, but it "
                    + forOperation.doesDescription() + ": " + operation);
        } else if (!forOperation.getClass().equals(fromConfigured.getClass())) {
            throw new FaultToleranceException("Configured fault tolerance '" + identifier
                    + "' expects the operation to " + fromConfigured.mustDescription()
                    + ", but it " + forOperation.doesDescription() + ": " + operation);
        } else {
            return faultTolerance.call(interceptionContext::proceed);
        }
    }

    private <V, AT> AT asyncFlow(FaultToleranceOperation operation, InvocationContext interceptionContext,
            InterceptionPoint point) {
        AsyncSupport<V, AT> asyncSupport = AsyncSupportRegistry.get(operation.getParameterTypes(), operation.getReturnType());
        if (asyncSupport == null) {
            throw new FaultToleranceException("Unknown async invocation: " + operation);
        }

        FaultToleranceStrategy<CompletionStage<V>> strategy = cache.getStrategy(point,
                () -> prepareAsyncStrategy(operation, point));

        Invoker<AT> invoker = new InterceptionInvoker<>(interceptionContext);

        io.smallrye.faulttolerance.core.InvocationContext<CompletionStage<V>> ctx = invocationContext(
                () -> asyncSupport.toCompletionStage(invoker), interceptionContext, operation);

        Invoker<CompletionStage<V>> wrapper = new StrategyInvoker<>(interceptionContext.getParameters(),
                strategy, ctx);
        try {
            return asyncSupport.fromCompletionStage(wrapper);
        } catch (Exception e) {
            throw sneakyThrow(e);
        }
    }

    private <T> T syncFlow(FaultToleranceOperation operation, InvocationContext interceptionContext, InterceptionPoint point)
            throws Exception {
        FaultToleranceStrategy<T> strategy = cache.getStrategy(point, () -> prepareSyncStrategy(operation, point));

        io.smallrye.faulttolerance.core.InvocationContext<T> ctx = invocationContext(
                () -> (T) interceptionContext.proceed(), interceptionContext, operation);

        return strategy.apply(ctx);
    }

    private <T> Future<T> futureFlow(FaultToleranceOperation operation, InvocationContext interceptionContext,
            InterceptionPoint point) throws Exception {
        FaultToleranceStrategy<Future<T>> strategy = cache.getStrategy(point, () -> prepareFutureStrategy(operation, point));

        io.smallrye.faulttolerance.core.InvocationContext<Future<T>> ctx = invocationContext(
                () -> (Future<T>) interceptionContext.proceed(), interceptionContext, operation);

        return strategy.apply(ctx);
    }

    private <T> io.smallrye.faulttolerance.core.InvocationContext<T> invocationContext(Callable<T> callable,
            InvocationContext interceptionContext, FaultToleranceOperation operation) {

        io.smallrye.faulttolerance.core.InvocationContext<T> result = new io.smallrye.faulttolerance.core.InvocationContext<>(
                callable);

        result.set(InvocationContext.class, interceptionContext);

        if (operation.hasCircuitBreaker() && operation.hasCircuitBreakerName()) {
            result.registerEventHandler(CircuitBreakerEvents.StateTransition.class,
                    cbMaintenance.stateTransitionEventHandler(operation.getCircuitBreakerName().value()));
        }

        return result;
    }

    private <T> FaultToleranceStrategy<CompletionStage<T>> prepareAsyncStrategy(FaultToleranceOperation operation,
            InterceptionPoint point) {
        FaultToleranceStrategy<CompletionStage<T>> result = invocation();

        result = new RequestScopeActivator<>(result, requestContextController);

        Executor executor = operation.isThreadOffloadRequired() ? asyncExecutor : DirectExecutor.INSTANCE;
        result = new CompletionStageExecution<>(result, executor);

        if (operation.hasBulkhead()) {
            int size = operation.getBulkhead().value();
            int queueSize = operation.getBulkhead().waitingTaskQueue();
            result = new CompletionStageThreadPoolBulkhead<>(result, point.toString(), size, queueSize);
        }

        if (operation.hasTimeout()) {
            long timeoutMs = getTimeInMs(operation.getTimeout().value(), operation.getTimeout().unit());
            result = new CompletionStageTimeout<>(result, point.toString(), timeoutMs,
                    new TimerTimeoutWatcher(timer));
        }

        if (operation.hasRateLimit()) {
            result = new CompletionStageRateLimit<>(result, point.toString(),
                    operation.getRateLimit().value(),
                    getTimeInMs(operation.getRateLimit().window(), operation.getRateLimit().windowUnit()),
                    getTimeInMs(operation.getRateLimit().minSpacing(), operation.getRateLimit().minSpacingUnit()),
                    operation.getRateLimit().type(),
                    SystemStopwatch.INSTANCE);
        }

        if (operation.hasCircuitBreaker()) {
            long delayInMillis = getTimeInMs(operation.getCircuitBreaker().delay(), operation.getCircuitBreaker().delayUnit());
            result = new CompletionStageCircuitBreaker<>(result, point.toString(),
                    createExceptionDecision(operation.getCircuitBreaker().skipOn(), operation.getCircuitBreaker().failOn()),
                    delayInMillis,
                    operation.getCircuitBreaker().requestVolumeThreshold(),
                    operation.getCircuitBreaker().failureRatio(),
                    operation.getCircuitBreaker().successThreshold(),
                    SystemStopwatch.INSTANCE);

            String cbName = operation.hasCircuitBreakerName()
                    ? operation.getCircuitBreakerName().value()
                    : UUID.randomUUID().toString();
            cbMaintenance.register(cbName, (CircuitBreaker<?>) result);
        }

        if (operation.hasRetry()) {
            long maxDurationMs = getTimeInMs(operation.getRetry().maxDuration(), operation.getRetry().durationUnit());

            Supplier<BackOff> backoff = prepareRetryBackoff(operation);

            result = new CompletionStageRetry<>(result, point.toString(),
                    createExceptionDecision(operation.getRetry().abortOn(), operation.getRetry().retryOn()),
                    operation.getRetry().maxRetries(),
                    maxDurationMs,
                    () -> new TimerDelay(backoff.get(), timer),
                    SystemStopwatch.INSTANCE);
        }

        if (operation.hasFallback()) {
            result = new CompletionStageFallback<>(result, point.toString(),
                    prepareFallbackFunction(point, operation),
                    createExceptionDecision(operation.getFallback().skipOn(), operation.getFallback().applyOn()));
        }

        if (metricsProvider.isEnabled()) {
            result = new CompletionStageMetricsCollector<>(result, getMetricsRecorder(operation, point),
                    operation.hasBulkhead(), operation.hasCircuitBreaker(), operation.hasRateLimit(),
                    operation.hasRetry(), operation.hasTimeout());
        }

        if (!operation.isThreadOffloadRequired()) {
            result = new RememberEventLoop<>(result, eventLoop);
        }

        return result;
    }

    private <T> FaultToleranceStrategy<T> prepareSyncStrategy(FaultToleranceOperation operation, InterceptionPoint point) {
        FaultToleranceStrategy<T> result = invocation();

        if (operation.hasBulkhead()) {
            result = new SemaphoreBulkhead<>(result, point.toString(), operation.getBulkhead().value());
        }

        if (operation.hasTimeout()) {
            long timeoutMs = getTimeInMs(operation.getTimeout().value(), operation.getTimeout().unit());
            result = new Timeout<>(result, point.toString(), timeoutMs,
                    new TimerTimeoutWatcher(timer));
        }

        if (operation.hasRateLimit()) {
            result = new RateLimit<>(result, point.toString(),
                    operation.getRateLimit().value(),
                    getTimeInMs(operation.getRateLimit().window(), operation.getRateLimit().windowUnit()),
                    getTimeInMs(operation.getRateLimit().minSpacing(), operation.getRateLimit().minSpacingUnit()),
                    operation.getRateLimit().type(),
                    SystemStopwatch.INSTANCE);
        }

        if (operation.hasCircuitBreaker()) {
            long delayInMillis = getTimeInMs(operation.getCircuitBreaker().delay(), operation.getCircuitBreaker().delayUnit());
            result = new CircuitBreaker<>(result, point.toString(),
                    createExceptionDecision(operation.getCircuitBreaker().skipOn(), operation.getCircuitBreaker().failOn()),
                    delayInMillis,
                    operation.getCircuitBreaker().requestVolumeThreshold(),
                    operation.getCircuitBreaker().failureRatio(),
                    operation.getCircuitBreaker().successThreshold(),
                    SystemStopwatch.INSTANCE);

            String cbName = operation.hasCircuitBreakerName()
                    ? operation.getCircuitBreakerName().value()
                    : UUID.randomUUID().toString();
            cbMaintenance.register(cbName, (CircuitBreaker<?>) result);
        }

        if (operation.hasRetry()) {
            long maxDurationMs = getTimeInMs(operation.getRetry().maxDuration(), operation.getRetry().durationUnit());

            Supplier<BackOff> backoff = prepareRetryBackoff(operation);

            result = new Retry<>(result, point.toString(),
                    createExceptionDecision(operation.getRetry().abortOn(), operation.getRetry().retryOn()),
                    operation.getRetry().maxRetries(),
                    maxDurationMs,
                    () -> new ThreadSleepDelay(backoff.get()),
                    SystemStopwatch.INSTANCE);
        }

        if (operation.hasFallback()) {
            result = new Fallback<>(result, point.toString(),
                    prepareFallbackFunction(point, operation),
                    createExceptionDecision(operation.getFallback().skipOn(), operation.getFallback().applyOn()));
        }

        if (metricsProvider.isEnabled()) {
            result = new MetricsCollector<>(result, getMetricsRecorder(operation, point), false,
                    operation.hasBulkhead(), operation.hasCircuitBreaker(), operation.hasRateLimit(),
                    operation.hasRetry(), operation.hasTimeout());
        }

        return result;
    }

    private <T> FaultToleranceStrategy<Future<T>> prepareFutureStrategy(FaultToleranceOperation operation,
            InterceptionPoint point) {
        FaultToleranceStrategy<Future<T>> result = invocation();

        result = new RequestScopeActivator<>(result, requestContextController);

        if (operation.hasBulkhead()) {
            int size = operation.getBulkhead().value();
            int queueSize = operation.getBulkhead().waitingTaskQueue();
            result = new FutureThreadPoolBulkhead<>(result, point.toString(), size, queueSize);
        }

        if (operation.hasTimeout()) {
            long timeoutMs = getTimeInMs(operation.getTimeout().value(), operation.getTimeout().unit());
            Timeout<Future<T>> timeout = new Timeout<>(result, point.toString(), timeoutMs,
                    new TimerTimeoutWatcher(timer));
            result = new AsyncTimeout<>(timeout, asyncExecutor);
        }

        if (operation.hasRateLimit()) {
            result = new RateLimit<>(result, point.toString(),
                    operation.getRateLimit().value(),
                    getTimeInMs(operation.getRateLimit().window(), operation.getRateLimit().windowUnit()),
                    getTimeInMs(operation.getRateLimit().minSpacing(), operation.getRateLimit().minSpacingUnit()),
                    operation.getRateLimit().type(),
                    SystemStopwatch.INSTANCE);
        }

        if (operation.hasCircuitBreaker()) {
            long delayInMillis = getTimeInMs(operation.getCircuitBreaker().delay(), operation.getCircuitBreaker().delayUnit());
            result = new CircuitBreaker<>(result, point.toString(),
                    createExceptionDecision(operation.getCircuitBreaker().skipOn(), operation.getCircuitBreaker().failOn()),
                    delayInMillis,
                    operation.getCircuitBreaker().requestVolumeThreshold(),
                    operation.getCircuitBreaker().failureRatio(),
                    operation.getCircuitBreaker().successThreshold(),
                    SystemStopwatch.INSTANCE);

            String cbName = operation.hasCircuitBreakerName()
                    ? operation.getCircuitBreakerName().value()
                    : UUID.randomUUID().toString();
            cbMaintenance.register(cbName, (CircuitBreaker<?>) result);
        }

        if (operation.hasRetry()) {
            long maxDurationMs = getTimeInMs(operation.getRetry().maxDuration(), operation.getRetry().durationUnit());

            Supplier<BackOff> backoff = prepareRetryBackoff(operation);

            result = new Retry<>(result, point.toString(),
                    createExceptionDecision(operation.getRetry().abortOn(), operation.getRetry().retryOn()),
                    operation.getRetry().maxRetries(),
                    maxDurationMs,
                    () -> new ThreadSleepDelay(backoff.get()),
                    SystemStopwatch.INSTANCE);
        }

        if (operation.hasFallback()) {
            result = new Fallback<>(result, point.toString(),
                    prepareFallbackFunction(point, operation),
                    createExceptionDecision(operation.getFallback().skipOn(), operation.getFallback().applyOn()));
        }

        if (metricsProvider.isEnabled()) {
            result = new MetricsCollector<>(result, getMetricsRecorder(operation, point), true,
                    operation.hasBulkhead(), operation.hasCircuitBreaker(), operation.hasRateLimit(),
                    operation.hasRetry(), operation.hasTimeout());
        }

        result = new FutureExecution<>(result, asyncExecutor);

        return result;
    }

    private Supplier<BackOff> prepareRetryBackoff(FaultToleranceOperation operation) {
        long delayMs = getTimeInMs(operation.getRetry().delay(), operation.getRetry().delayUnit());

        long jitterMs = getTimeInMs(operation.getRetry().jitter(), operation.getRetry().jitterDelayUnit());
        Jitter jitter = jitterMs == 0 ? Jitter.ZERO : new RandomJitter(jitterMs);

        if (operation.hasExponentialBackoff()) {
            int factor = operation.getExponentialBackoff().factor();
            long maxDelay = getTimeInMs(operation.getExponentialBackoff().maxDelay(),
                    operation.getExponentialBackoff().maxDelayUnit());
            return () -> new ExponentialBackOff(delayMs, factor, jitter, maxDelay);
        } else if (operation.hasFibonacciBackoff()) {
            long maxDelay = getTimeInMs(operation.getFibonacciBackoff().maxDelay(),
                    operation.getFibonacciBackoff().maxDelayUnit());
            return () -> new FibonacciBackOff(delayMs, jitter, maxDelay);
        } else if (operation.hasCustomBackoff()) {
            Class<? extends CustomBackoffStrategy> strategy = operation.getCustomBackoff().value();
            return () -> {
                CustomBackoffStrategy instance;
                try {
                    instance = strategy.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw sneakyThrow(e);
                }
                instance.init(delayMs);
                return new CustomBackOff(instance::nextDelayInMillis);
            };
        } else {
            return () -> new ConstantBackOff(delayMs, jitter);
        }
    }

    private <V> FallbackFunction<V> prepareFallbackFunction(InterceptionPoint point, FaultToleranceOperation operation) {
        Method fallbackMethod = null;

        Class<? extends FallbackHandler<?>> fallback = operation.getFallback().value();
        String fallbackMethodName = operation.getFallback().fallbackMethod();

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

        AsyncSupport asyncSupport = AsyncSupportRegistry.get(operation.getParameterTypes(), operation.getReturnType());

        FallbackFunction<V> fallbackFunction;

        Method fallbackMethodFinal = fallbackMethod;
        if (fallbackMethod != null) {
            boolean isDefault = fallbackMethodFinal.isDefault();
            fallbackFunction = ctx -> {
                InvocationContext interceptionContext = ctx.invocationContext.get(InvocationContext.class);
                try {
                    Invoker invoker = isDefault
                            ? new SpecialMethodInvoker(fallbackMethodFinal, interceptionContext.getTarget(),
                                    interceptionContext.getParameters())
                            : new NormalMethodInvoker(fallbackMethodFinal, interceptionContext.getTarget(),
                                    interceptionContext.getParameters());
                    return asyncSupport == null ? (V) invoker.proceed() : (V) asyncSupport.toCompletionStage(invoker);
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
                fallbackFunction = ctx -> {
                    InvocationContext interceptionContext = ctx.invocationContext.get(InvocationContext.class);
                    ExecutionContextWithInvocationContext executionContext = new ExecutionContextWithInvocationContext(
                            interceptionContext);
                    executionContext.setFailure(ctx.failure);
                    V result = fallbackHandler.handle(executionContext);
                    if (asyncSupport != null) {
                        result = (V) asyncSupport.fallbackResultToCompletionStage(result);
                    }
                    return result;
                };
            } else {
                throw new FaultToleranceException("Could not obtain fallback handler for " + point);
            }
        }

        if (specCompatibility.isOperationTrulyAsynchronous(operation) && operation.isThreadOffloadRequired()) {
            fallbackFunction = new AsyncFallbackFunction(fallbackFunction, asyncExecutor);
        }

        return fallbackFunction;
    }

    private long getTimeInMs(long time, ChronoUnit unit) {
        return Duration.of(time, unit).toMillis();
    }

    private ExceptionDecision createExceptionDecision(Class<? extends Throwable>[] consideredExpected,
            Class<? extends Throwable>[] consideredFailure) {
        return new SetBasedExceptionDecision(createSetOfThrowables(consideredFailure),
                createSetOfThrowables(consideredExpected), specCompatibility.inspectExceptionCauseChain());
    }

    private SetOfThrowables createSetOfThrowables(Class<? extends Throwable>[] throwableClasses) {
        if (throwableClasses == null) {
            return SetOfThrowables.EMPTY;
        }
        return SetOfThrowables.create(Arrays.asList(throwableClasses));
    }

    private MetricsRecorder getMetricsRecorder(FaultToleranceOperation operation, InterceptionPoint point) {
        return cache.getMetrics(point, () -> metricsProvider.create(operation));
    }
}
