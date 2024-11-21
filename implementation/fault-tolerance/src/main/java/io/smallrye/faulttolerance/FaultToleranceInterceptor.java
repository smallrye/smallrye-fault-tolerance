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
import static io.smallrye.faulttolerance.core.util.Durations.timeInMillis;
import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.control.RequestContextController;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Intercepted;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.AlwaysOnException;
import io.smallrye.faulttolerance.api.BeforeRetryHandler;
import io.smallrye.faulttolerance.api.CustomBackoffStrategy;
import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.api.Guard;
import io.smallrye.faulttolerance.api.NeverOnResult;
import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;
import io.smallrye.faulttolerance.core.FailureContext;
import io.smallrye.faulttolerance.core.FaultToleranceContext;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.apiimpl.GuardImpl;
import io.smallrye.faulttolerance.core.apiimpl.LazyFaultTolerance;
import io.smallrye.faulttolerance.core.apiimpl.LazyGuard;
import io.smallrye.faulttolerance.core.apiimpl.LazyTypedGuard;
import io.smallrye.faulttolerance.core.apiimpl.TypedGuardImpl;
import io.smallrye.faulttolerance.core.async.FutureExecution;
import io.smallrye.faulttolerance.core.async.RememberEventLoop;
import io.smallrye.faulttolerance.core.async.ThreadOffload;
import io.smallrye.faulttolerance.core.async.ThreadOffloadEnabled;
import io.smallrye.faulttolerance.core.bulkhead.Bulkhead;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreaker;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerEvents;
import io.smallrye.faulttolerance.core.event.loop.EventLoop;
import io.smallrye.faulttolerance.core.fallback.Fallback;
import io.smallrye.faulttolerance.core.fallback.FallbackFunction;
import io.smallrye.faulttolerance.core.fallback.ThreadOffloadFallbackFunction;
import io.smallrye.faulttolerance.core.invocation.AsyncSupport;
import io.smallrye.faulttolerance.core.invocation.AsyncSupportRegistry;
import io.smallrye.faulttolerance.core.invocation.ConstantInvoker;
import io.smallrye.faulttolerance.core.invocation.Invoker;
import io.smallrye.faulttolerance.core.invocation.StrategyInvoker;
import io.smallrye.faulttolerance.core.metrics.MeteredOperation;
import io.smallrye.faulttolerance.core.metrics.MeteredOperationName;
import io.smallrye.faulttolerance.core.metrics.MetricsCollector;
import io.smallrye.faulttolerance.core.metrics.MetricsProvider;
import io.smallrye.faulttolerance.core.rate.limit.RateLimit;
import io.smallrye.faulttolerance.core.retry.BackOff;
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
import io.smallrye.faulttolerance.core.timeout.FutureTimeout;
import io.smallrye.faulttolerance.core.timeout.Timeout;
import io.smallrye.faulttolerance.core.timer.Timer;
import io.smallrye.faulttolerance.core.util.ExceptionDecision;
import io.smallrye.faulttolerance.core.util.PredicateBasedExceptionDecision;
import io.smallrye.faulttolerance.core.util.PredicateBasedResultDecision;
import io.smallrye.faulttolerance.core.util.ResultDecision;
import io.smallrye.faulttolerance.core.util.SetBasedExceptionDecision;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;
import io.smallrye.faulttolerance.internal.BeforeRetryMethod;
import io.smallrye.faulttolerance.internal.FallbackMethod;
import io.smallrye.faulttolerance.internal.FallbackMethodCandidates;
import io.smallrye.faulttolerance.internal.InterceptionInvoker;
import io.smallrye.faulttolerance.internal.InterceptionPoint;
import io.smallrye.faulttolerance.internal.RequestScopeActivator;
import io.smallrye.faulttolerance.internal.StrategyCache;
import io.smallrye.faulttolerance.metrics.CdiMeteredOperationImpl;

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

    private final BeforeRetryHandlerProvider beforeRetryHandlerProvider;

    private final MetricsProvider metricsProvider;

    private final ExecutorService asyncExecutor;

    private final EventLoop eventLoop;

    private final Timer timer;

    private final RequestContextController requestContextController;

    private final CircuitBreakerMaintenanceImpl cbMaintenance;

    private final SpecCompatibility specCompatibility;

    private final Instance<FaultTolerance<?>> configuredFaultTolerance;

    private final Instance<Guard> configuredGuard;

    private final Instance<TypedGuard<?>> configuredTypedGuard;

    @Inject
    public FaultToleranceInterceptor(
            @Intercepted Bean<?> interceptedBean,
            FaultToleranceOperationProvider operationProvider,
            StrategyCache cache,
            FallbackHandlerProvider fallbackHandlerProvider,
            BeforeRetryHandlerProvider beforeRetryHandlerProvider,
            MetricsProvider metricsProvider,
            ExecutorHolder executorHolder,
            RequestContextIntegration requestContextIntegration,
            CircuitBreakerMaintenanceImpl cbMaintenance,
            SpecCompatibility specCompatibility,
            @Any Instance<FaultTolerance<?>> configuredFaultTolerance,
            @Any Instance<Guard> configuredGuard,
            @Any Instance<TypedGuard<?>> configuredTypedGuard) {
        this.interceptedBean = interceptedBean;
        this.operationProvider = operationProvider;
        this.cache = cache;
        this.fallbackHandlerProvider = fallbackHandlerProvider;
        this.beforeRetryHandlerProvider = beforeRetryHandlerProvider;
        this.metricsProvider = metricsProvider;
        asyncExecutor = executorHolder.getAsyncExecutor();
        eventLoop = executorHolder.getEventLoop();
        timer = executorHolder.getTimer();
        requestContextController = requestContextIntegration.get();
        this.cbMaintenance = cbMaintenance;
        this.specCompatibility = specCompatibility;
        this.configuredFaultTolerance = configuredFaultTolerance;
        this.configuredGuard = configuredGuard;
        this.configuredTypedGuard = configuredTypedGuard;
    }

    @AroundInvoke
    public Object intercept(InvocationContext invocationContext) throws Throwable {
        Method method = invocationContext.getMethod();
        Class<?> beanClass = interceptedBean != null ? interceptedBean.getBeanClass() : method.getDeclaringClass();
        InterceptionPoint point = new InterceptionPoint(beanClass, invocationContext.getMethod());
        FaultToleranceOperation operation = operationProvider.get(beanClass, method);

        if (operation.hasApplyFaultTolerance()) {
            return applyFaultToleranceFlow(operation, invocationContext);
        } else if (operation.hasApplyGuard()) {
            return applyGuardFlow(operation, invocationContext, point);
        } else if (specCompatibility.isOperationTrulyAsynchronous(operation)) {
            return asyncFlow(operation, invocationContext, point);
        } else if (specCompatibility.isOperationPseudoAsynchronous(operation)) {
            return futureFlow(operation, invocationContext, point);
        } else {
            return syncFlow(operation, invocationContext, point);
        }
    }

    // V = value type, e.g. String
    // T = result type, e.g. String or CompletionStage<String> or Uni<String>
    //
    // in synchronous scenario, V = T
    // in asynchronous scenario, T is an async type that eventually produces V
    private <V, T> T applyFaultToleranceFlow(FaultToleranceOperation operation, InvocationContext invocationContext)
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
        LazyFaultTolerance<Object> lazyFaultTolerance = (LazyFaultTolerance<Object>) faultTolerance;

        Class<?> asyncType = lazyFaultTolerance.internalGetAsyncType();
        MeteredOperationName meteredOperationName = new MeteredOperationName(operation.getName());

        AsyncSupport<?, ?> forOperation = AsyncSupportRegistry.get(operation.getParameterTypes(), operation.getReturnType());
        AsyncSupport<?, ?> fromConfigured = asyncType == null ? null : AsyncSupportRegistry.get(new Class[0], asyncType);

        if (forOperation == null && fromConfigured == null) {
            return (T) lazyFaultTolerance.call(invocationContext::proceed, meteredOperationName);
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
            return (T) lazyFaultTolerance.call(invocationContext::proceed, meteredOperationName);
        }
    }

    // V = value type, e.g. String
    // T = result type, e.g. String or CompletionStage<String> or Uni<String>
    //
    // in synchronous scenario, V = T
    // in asynchronous scenario, T is an async type that eventually produces V
    private <V, T> T applyGuardFlow(FaultToleranceOperation operation, InvocationContext invocationContext,
            InterceptionPoint point) throws Exception {
        String identifier = operation.getApplyGuard().value();
        Instance<Guard> guardInstance = configuredGuard.select(Identifier.Literal.of(identifier));
        Instance<TypedGuard<T>> typedGuardInstance = (Instance) configuredTypedGuard.select(Identifier.Literal.of(identifier));
        // at least one of them should be resolvable, otherwise a deployment problem has occurred
        // the check here is redundant, but we keep it just in case
        if (!guardInstance.isResolvable() && !typedGuardInstance.isResolvable()) {
            throw new FaultToleranceException("Can't resolve a bean of type " + Guard.class.getName()
                    + " or " + TypedGuard.class.getName()
                    + " with qualifier @" + Identifier.class.getName() + "(\"" + identifier + "\")");
        }

        FallbackFunction<V> fallbackFunction;
        ExceptionDecision exceptionDecision;
        if (operation.hasFallback()) {
            fallbackFunction = cache.getFallbackFunction(point,
                    () -> prepareFallbackFunction(point, operation));
            exceptionDecision = cache.getFallbackExceptionDecision(point,
                    () -> createExceptionDecision(operation.getFallback().skipOn(), operation.getFallback().applyOn()));
        } else {
            fallbackFunction = null;
            exceptionDecision = null;
        }
        Boolean threadOffload;
        if (specCompatibility.isOperationTrulyAsynchronous(operation)) {
            threadOffload = operation.isThreadOffloadRequired();
        } else {
            threadOffload = null;
        }
        MeteredOperationName meteredOperationName = new MeteredOperationName(operation.getName());

        Consumer<FaultToleranceContext<?>> contextModifier = ctx -> {
            ctx.set(InvocationContext.class, invocationContext);

            if (fallbackFunction != null) {
                ctx.set(FallbackFunction.class, fallbackFunction);
            }
            if (exceptionDecision != null) {
                ctx.set(ExceptionDecision.class, exceptionDecision);
            }
            if (threadOffload != null) {
                ctx.set(ThreadOffloadEnabled.class, new ThreadOffloadEnabled(threadOffload));
            }
            ctx.set(MeteredOperationName.class, meteredOperationName);
        };

        if (guardInstance.isResolvable()) {
            Guard guard = guardInstance.get();
            if (!(guard instanceof LazyGuard)) {
                throw new FaultToleranceException("Configured Guard '" + identifier
                        + "' is not created by the Guard API, this is not supported");
            }
            GuardImpl guardImpl = ((LazyGuard) guard).instance();

            return guardImpl.guard(() -> (T) invocationContext.proceed(), operation.getReturnType(), contextModifier);
        } else /* typedGuardInstance.isResolvable() */ {
            TypedGuard<T> guard = typedGuardInstance.get();
            if (!(guard instanceof LazyTypedGuard)) {
                throw new FaultToleranceException("Configured TypedGuard '" + identifier
                        + "' is not created by the TypedGuard API, this is not supported");
            }
            TypedGuardImpl<V, T> guardImpl = ((LazyTypedGuard<V, T>) guard).instance();

            return guardImpl.guard(() -> (T) invocationContext.proceed(), contextModifier);
        }
    }

    // V = value type, e.g. String
    // AT = async type that eventually produces V, e.g. CompletionStage<String> or Uni<String>
    private <V, AT> AT asyncFlow(FaultToleranceOperation operation, InvocationContext invocationContext,
            InterceptionPoint point) {
        AsyncSupport<V, AT> asyncSupport = cache.getAsyncSupport(point, operation);
        if (asyncSupport == null) {
            throw new FaultToleranceException("Unknown async invocation: " + operation);
        }

        FaultToleranceStrategy<V> strategy = cache.getStrategy(point, () -> prepareStrategy(operation, point));

        Invoker<AT> invoker = new InterceptionInvoker<>(invocationContext);

        FaultToleranceContext<V> ctx = faultToleranceContext(() -> asyncSupport.toFuture(invoker), invocationContext,
                operation);

        Invoker<Future<V>> wrapper = new StrategyInvoker<>(invocationContext.getParameters(), strategy, ctx);
        return asyncSupport.fromFuture(wrapper);
    }

    private <V> V syncFlow(FaultToleranceOperation operation, InvocationContext invocationContext, InterceptionPoint point)
            throws Throwable {
        FaultToleranceStrategy<V> strategy = cache.getStrategy(point, () -> prepareStrategy(operation, point));

        FaultToleranceContext<V> ctx = faultToleranceContext(
                () -> Future.from(() -> (V) invocationContext.proceed()),
                invocationContext, operation);

        return strategy.apply(ctx).awaitBlocking();
    }

    private <V> java.util.concurrent.Future<V> futureFlow(FaultToleranceOperation operation,
            InvocationContext invocationContext, InterceptionPoint point) throws Throwable {
        FaultToleranceStrategy<java.util.concurrent.Future<V>> strategy = cache.getStrategy(point,
                () -> prepareFutureStrategy(operation, point));

        FaultToleranceContext<java.util.concurrent.Future<V>> ctx = faultToleranceContext(
                () -> Future.from(() -> (java.util.concurrent.Future<V>) invocationContext.proceed()),
                invocationContext, operation);

        try {
            // blocking is OK here because the first strategy in the chain, `FutureExecution`,
            // returns an immediately complete `Future` that contains the `java.util.concurrent.Future`
            return strategy.apply(ctx).awaitBlocking();
        } catch (Throwable e) {
            throw new ExecutionException(e);
        }
    }

    private <T> FaultToleranceContext<T> faultToleranceContext(Supplier<Future<T>> callable,
            InvocationContext invocationContext, FaultToleranceOperation operation) {

        FaultToleranceContext<T> result = new FaultToleranceContext<>(callable,
                specCompatibility.isOperationTrulyAsynchronous(operation));

        result.set(InvocationContext.class, invocationContext);

        if (operation.hasCircuitBreaker() && operation.hasCircuitBreakerName()) {
            result.registerEventHandler(CircuitBreakerEvents.StateTransition.class,
                    cbMaintenance.stateTransitionEventHandler(operation.getCircuitBreakerName().value()));
        }

        return result;
    }

    private <T> FaultToleranceStrategy<T> prepareStrategy(FaultToleranceOperation operation, InterceptionPoint point) {
        FaultToleranceStrategy<T> result = invocation();

        if (specCompatibility.isOperationTrulyAsynchronous(operation)) {
            result = new RequestScopeActivator<>(result, requestContextController);
        }

        if (specCompatibility.isOperationTrulyAsynchronous(operation) && operation.isThreadOffloadRequired()) {
            result = new ThreadOffload<>(result, asyncExecutor);
        }

        if (operation.hasBulkhead()) {
            result = new Bulkhead<>(result, point.toString(),
                    operation.getBulkhead().value(),
                    operation.getBulkhead().waitingTaskQueue(),
                    false);
        }

        if (operation.hasTimeout()) {
            result = new Timeout<>(result, point.toString(),
                    timeInMillis(operation.getTimeout().value(), operation.getTimeout().unit()),
                    timer);
        }

        if (operation.hasRateLimit()) {
            result = new RateLimit<>(result, point.toString(),
                    operation.getRateLimit().value(),
                    timeInMillis(operation.getRateLimit().window(), operation.getRateLimit().windowUnit()),
                    timeInMillis(operation.getRateLimit().minSpacing(), operation.getRateLimit().minSpacingUnit()),
                    operation.getRateLimit().type(),
                    SystemStopwatch.INSTANCE);
        }

        if (operation.hasCircuitBreaker()) {
            result = new CircuitBreaker<>(result, point.toString(),
                    createExceptionDecision(operation.getCircuitBreaker().skipOn(), operation.getCircuitBreaker().failOn()),
                    timeInMillis(operation.getCircuitBreaker().delay(), operation.getCircuitBreaker().delayUnit()),
                    operation.getCircuitBreaker().requestVolumeThreshold(),
                    operation.getCircuitBreaker().failureRatio(),
                    operation.getCircuitBreaker().successThreshold(),
                    SystemStopwatch.INSTANCE,
                    timer);

            String cbName = operation.hasCircuitBreakerName()
                    ? operation.getCircuitBreakerName().value()
                    : UUID.randomUUID().toString();
            cbMaintenance.register(cbName, (CircuitBreaker<?>) result);
        }

        if (operation.hasRetry()) {
            Supplier<BackOff> backoff = prepareRetryBackoff(operation);

            result = new Retry<>(result, point.toString(),
                    createResultDecision(operation.hasRetryWhen() ? operation.getRetryWhen().result() : null),
                    createExceptionDecision(operation.getRetry().abortOn(), operation.getRetry().retryOn(),
                            operation.hasRetryWhen() ? operation.getRetryWhen().exception() : null),
                    operation.getRetry().maxRetries(),
                    timeInMillis(operation.getRetry().maxDuration(), operation.getRetry().durationUnit()),
                    () -> new ThreadSleepDelay(backoff.get()),
                    () -> new TimerDelay(backoff.get(), timer),
                    SystemStopwatch.INSTANCE,
                    operation.hasBeforeRetry() ? prepareBeforeRetryFunction(point, operation) : null);
        }

        if (operation.hasFallback()) {
            result = new Fallback<>(result, point.toString(),
                    prepareFallbackFunction(point, operation),
                    createExceptionDecision(operation.getFallback().skipOn(), operation.getFallback().applyOn()));
        }

        if (metricsProvider.isEnabled()) {
            MeteredOperation meteredOperation = new CdiMeteredOperationImpl(operation, point, specCompatibility);
            result = new MetricsCollector<>(result, metricsProvider.create(meteredOperation), meteredOperation);
        }

        if (specCompatibility.isOperationTrulyAsynchronous(operation) && !operation.isThreadOffloadRequired()) {
            result = new RememberEventLoop<>(result, eventLoop);
        }

        return result;
    }

    private <T> FaultToleranceStrategy<java.util.concurrent.Future<T>> prepareFutureStrategy(FaultToleranceOperation operation,
            InterceptionPoint point) {
        FaultToleranceStrategy<java.util.concurrent.Future<T>> result = invocation();

        result = new RequestScopeActivator<>(result, requestContextController);

        if (operation.hasBulkhead()) {
            result = new Bulkhead<>(result, point.toString(),
                    operation.getBulkhead().value(),
                    operation.getBulkhead().waitingTaskQueue(),
                    true);
        }

        if (operation.hasTimeout()) {
            Timeout<java.util.concurrent.Future<T>> timeout = new Timeout<>(result, point.toString(),
                    timeInMillis(operation.getTimeout().value(), operation.getTimeout().unit()),
                    timer);
            result = new FutureTimeout<>(timeout, asyncExecutor);
        }

        if (operation.hasRateLimit()) {
            result = new RateLimit<>(result, point.toString(),
                    operation.getRateLimit().value(),
                    timeInMillis(operation.getRateLimit().window(), operation.getRateLimit().windowUnit()),
                    timeInMillis(operation.getRateLimit().minSpacing(), operation.getRateLimit().minSpacingUnit()),
                    operation.getRateLimit().type(),
                    SystemStopwatch.INSTANCE);
        }

        if (operation.hasCircuitBreaker()) {
            result = new CircuitBreaker<>(result, point.toString(),
                    createExceptionDecision(operation.getCircuitBreaker().skipOn(), operation.getCircuitBreaker().failOn()),
                    timeInMillis(operation.getCircuitBreaker().delay(), operation.getCircuitBreaker().delayUnit()),
                    operation.getCircuitBreaker().requestVolumeThreshold(),
                    operation.getCircuitBreaker().failureRatio(),
                    operation.getCircuitBreaker().successThreshold(),
                    SystemStopwatch.INSTANCE,
                    timer);

            String cbName = operation.hasCircuitBreakerName()
                    ? operation.getCircuitBreakerName().value()
                    : UUID.randomUUID().toString();
            cbMaintenance.register(cbName, (CircuitBreaker<?>) result);
        }

        if (operation.hasRetry()) {
            Supplier<BackOff> backoff = prepareRetryBackoff(operation);

            result = new Retry<>(result, point.toString(),
                    createResultDecision(operation.hasRetryWhen() ? operation.getRetryWhen().result() : null),
                    createExceptionDecision(operation.getRetry().abortOn(), operation.getRetry().retryOn(),
                            operation.hasRetryWhen() ? operation.getRetryWhen().exception() : null),
                    operation.getRetry().maxRetries(),
                    timeInMillis(operation.getRetry().maxDuration(), operation.getRetry().durationUnit()),
                    () -> new ThreadSleepDelay(backoff.get()),
                    () -> new TimerDelay(backoff.get(), timer),
                    SystemStopwatch.INSTANCE,
                    operation.hasBeforeRetry() ? prepareBeforeRetryFunction(point, operation) : null);
        }

        if (operation.hasFallback()) {
            result = new Fallback<>(result, point.toString(),
                    prepareFallbackFunction(point, operation),
                    createExceptionDecision(operation.getFallback().skipOn(), operation.getFallback().applyOn()));
        }

        if (metricsProvider.isEnabled()) {
            MeteredOperation meteredOperation = new CdiMeteredOperationImpl(operation, point, specCompatibility);
            result = new MetricsCollector<>(result, metricsProvider.create(meteredOperation), meteredOperation);
        }

        result = new FutureExecution<>(result, asyncExecutor);

        return result;
    }

    private Supplier<BackOff> prepareRetryBackoff(FaultToleranceOperation operation) {
        long delayMs = timeInMillis(operation.getRetry().delay(), operation.getRetry().delayUnit());

        long jitterMs = timeInMillis(operation.getRetry().jitter(), operation.getRetry().jitterDelayUnit());
        Jitter jitter = jitterMs == 0 ? Jitter.ZERO : new RandomJitter(jitterMs);

        if (operation.hasExponentialBackoff()) {
            int factor = operation.getExponentialBackoff().factor();
            long maxDelay = timeInMillis(operation.getExponentialBackoff().maxDelay(),
                    operation.getExponentialBackoff().maxDelayUnit());
            return () -> new ExponentialBackOff(delayMs, factor, jitter, maxDelay);
        } else if (operation.hasFibonacciBackoff()) {
            long maxDelay = timeInMillis(operation.getFibonacciBackoff().maxDelay(),
                    operation.getFibonacciBackoff().maxDelayUnit());
            return () -> new FibonacciBackOff(delayMs, jitter, maxDelay);
        } else if (operation.hasCustomBackoff()) {
            Class<? extends CustomBackoffStrategy> strategy = operation.getCustomBackoff().value();
            return () -> {
                CustomBackoffStrategy instance;
                try {
                    instance = strategy.getConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    throw sneakyThrow(e);
                }
                instance.init(delayMs);
                return new CustomBackOff(instance::nextDelayInMillis);
            };
        } else {
            return () -> new ConstantBackOff(delayMs, jitter);
        }
    }

    // V = value type, e.g. String
    // T = result type, e.g. String or CompletionStage<String> or Uni<String>
    //
    // in synchronous scenario, V = T
    // in asynchronous scenario, T is an async type that eventually produces V
    private <V, T> FallbackFunction<V> prepareFallbackFunction(
            InterceptionPoint point, FaultToleranceOperation operation) {
        AsyncSupport<V, T> asyncSupport = cache.getAsyncSupport(point, operation);

        String fallbackMethodName = operation.getFallback().fallbackMethod();
        FallbackMethodCandidates candidates = !"".equals(fallbackMethodName)
                ? cache.getFallbackMethodCandidates(point, operation)
                : null;

        FallbackFunction<V> fallbackFunction;

        if (candidates != null) {
            fallbackFunction = ctx -> {
                FallbackMethod fallbackMethod = candidates.select(ctx.failure.getClass());
                if (fallbackMethod == null) {
                    return Future.ofError(ctx.failure);
                }

                try {
                    Invoker<T> invoker = fallbackMethod.createInvoker(ctx);
                    return asyncSupport == null
                            ? Future.of((V) invoker.proceed())
                            : (Future<V>) asyncSupport.toFuture(invoker);
                } catch (InvocationTargetException e) {
                    return Future.ofError(e.getCause());
                } catch (Exception e) {
                    return Future.ofError(new FaultToleranceException("Error during fallback method invocation", e));
                }
            };
        } else {
            FallbackHandler<T> fallbackHandler = fallbackHandlerProvider.get(operation);
            if (fallbackHandler != null) {
                fallbackFunction = ctx -> {
                    ExecutionContext executionContext = new ExecutionContextImpl(
                            ctx.context.get(InvocationContext.class), ctx.failure);
                    try {
                        T result = fallbackHandler.handle(executionContext);
                        return asyncSupport == null
                                ? Future.of((V) result)
                                : asyncSupport.toFuture(ConstantInvoker.of(result));
                    } catch (Exception e) {
                        return Future.ofError(e);
                    }
                };
            } else {
                throw new FaultToleranceException("Could not obtain fallback handler for " + point);
            }
        }

        if (specCompatibility.isOperationTrulyAsynchronous(operation) && operation.isThreadOffloadRequired()) {
            fallbackFunction = new ThreadOffloadFallbackFunction<>(fallbackFunction, asyncExecutor);
        }

        return fallbackFunction;
    }

    private Consumer<FailureContext> prepareBeforeRetryFunction(InterceptionPoint point, FaultToleranceOperation operation) {
        String methodName = operation.getBeforeRetry().methodName();
        BeforeRetryMethod method = !"".equals(methodName)
                ? cache.getBeforeRetryMethod(point, operation)
                : null;

        Consumer<FailureContext> beforeRetryFunction;

        if (method != null) {
            beforeRetryFunction = ctx -> {
                try {
                    method.createInvoker(ctx).proceed();
                } catch (InvocationTargetException e) {
                    throw sneakyThrow(e.getCause());
                } catch (Throwable e) {
                    throw new FaultToleranceException("Error during before retry method invocation", e);
                }
            };
        } else {
            BeforeRetryHandler beforeRetryHandler = beforeRetryHandlerProvider.get(operation);
            if (beforeRetryHandler != null) {
                beforeRetryFunction = ctx -> {
                    ExecutionContext executionContext = new ExecutionContextImpl(
                            ctx.context.get(InvocationContext.class), ctx.failure);
                    beforeRetryHandler.handle(executionContext);
                };
            } else {
                throw new FaultToleranceException("Could not obtain before retry handler for " + point);
            }
        }

        return beforeRetryFunction;
    }

    private ResultDecision createResultDecision(Class<? extends Predicate<Object>> whenResult) {
        if (whenResult != null && whenResult != NeverOnResult.class) {
            Predicate<Object> predicate;
            try {
                predicate = whenResult.getConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw sneakyThrow(e);
            }
            return new PredicateBasedResultDecision(predicate.negate());
        }
        return ResultDecision.ALWAYS_EXPECTED;
    }

    private ExceptionDecision createExceptionDecision(Class<? extends Throwable>[] consideredExpected,
            Class<? extends Throwable>[] consideredFailure) {
        return new SetBasedExceptionDecision(createSetOfThrowables(consideredFailure),
                createSetOfThrowables(consideredExpected), specCompatibility.inspectExceptionCauseChain());
    }

    private ExceptionDecision createExceptionDecision(Class<? extends Throwable>[] consideredExpected,
            Class<? extends Throwable>[] consideredFailure, Class<? extends Predicate<Throwable>> whenException) {
        if (whenException != null && whenException != AlwaysOnException.class) {
            Predicate<Throwable> predicate;
            try {
                predicate = whenException.getConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw sneakyThrow(e);
            }
            return new PredicateBasedExceptionDecision(predicate.negate());
        }
        return new SetBasedExceptionDecision(createSetOfThrowables(consideredFailure),
                createSetOfThrowables(consideredExpected), specCompatibility.inspectExceptionCauseChain());
    }

    private SetOfThrowables createSetOfThrowables(Class<? extends Throwable>[] throwableClasses) {
        if (throwableClasses == null || throwableClasses.length == 0) {
            return SetOfThrowables.EMPTY;
        }
        return SetOfThrowables.create(Arrays.asList(throwableClasses));
    }
}
