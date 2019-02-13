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

import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommand.Setter;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.netflix.hystrix.exception.HystrixRuntimeException.FailureType;
import io.smallrye.faulttolerance.config.BulkheadConfig;
import io.smallrye.faulttolerance.config.CircuitBreakerConfig;
import io.smallrye.faulttolerance.config.FallbackConfig;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;
import io.smallrye.faulttolerance.config.TimeoutConfig;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.jboss.logging.Logger;
import rx.Observable;
import rx.Subscription;

import javax.annotation.Priority;
import javax.enterprise.inject.Intercepted;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.PrivilegedActionException;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * <h2>Implementation notes:</h2>
 * <p>
 * If {@link SynchronousCircuitBreaker} is used it is not possible to track the execution inside a Hystrix command because a {@link TimeoutException} should be
 * always counted as a failure, even if the command execution completes normally.
 * </p>
 * <p>
 * We never use {@link HystrixCommand#queue()} for async execution. Mostly to workaround various problems of {@link Asynchronous} {@link Retry} combination.
 * Instead, we create a composite command and inside its run() method we execute commands synchronously.
 * </p>
 *
 * @author Antoine Sabot-Durand
 * @author Martin Kouba
 */
@Interceptor
@HystrixCommandBinding
@Priority(Interceptor.Priority.PLATFORM_AFTER + 10)
public class HystrixCommandInterceptor {

    /**
     * This config property key can be used to disable synchronous circuit breaker functionality. If disabled, {@link CircuitBreaker#successThreshold()} of
     * value greater than 1 is not supported. Also the {@link CircuitBreaker#failOn()} configuration is ignored.
     * <p>
     * Moreover, circuit breaker does not necessarily transition from CLOSED to OPEN immediately when a fault tolerance operation completes. See also
     * <a href="https://github.com/Netflix/Hystrix/wiki/Configuration#metrics.healthSnapshot.intervalInMilliseconds">Hystrix configuration</a>
     * </p>
     * <p>
     * In general, application developers are encouraged to disable this feature on high-volume circuits and in production environments.
     * </p>
     */
    public static final String SYNC_CIRCUIT_BREAKER_KEY = "io_smallrye_faulttolerance_syncCircuitBreaker";

    private static final Logger LOGGER = Logger.getLogger(HystrixCommandInterceptor.class);

    private final ConcurrentMap<String, HystrixCircuitBreaker> circuitBreakers;

    private final ConcurrentMap<Method, CommandMetadata> commandMetadataCache;

    private final Boolean nonFallBackEnable;

    private final Boolean syncCircuitBreakerEnabled;

    private final FallbackHandlerProvider fallbackHandlerProvider;

    private final FaultToleranceOperationProvider faultToleranceOperationProvider;

    private final CommandListenersProvider listenersProvider;

    private final Bean<?> interceptedBean;

    private final MetricsCollectorFactory metricsCollectorFactory;

    @SuppressWarnings("unchecked")
    @Inject
    public HystrixCommandInterceptor(@ConfigProperty(name = "MP_Fault_Tolerance_NonFallback_Enabled", defaultValue = "true") Boolean nonFallBackEnable,
            Config config, FallbackHandlerProvider fallbackHandlerProvider, FaultToleranceOperationProvider faultToleranceOperationProvider,
            CommandListenersProvider listenersProvider, @Intercepted Bean<?> interceptedBean, MetricsCollectorFactory metricsCollectorFactory) {
        this.nonFallBackEnable = nonFallBackEnable;
        this.syncCircuitBreakerEnabled = config.getOptionalValue(SYNC_CIRCUIT_BREAKER_KEY, Boolean.class).orElse(true);
        this.fallbackHandlerProvider = fallbackHandlerProvider;
        this.faultToleranceOperationProvider = faultToleranceOperationProvider;
        this.commandMetadataCache = new ConcurrentHashMap<>();
        this.listenersProvider = listenersProvider;
        this.interceptedBean = interceptedBean;
        this.metricsCollectorFactory = metricsCollectorFactory;
        // WORKAROUND: Hystrix does not allow integrators to use a custom HystrixCircuitBreaker impl
        // See also https://github.com/Netflix/Hystrix/issues/9
        try {
            Field field = SecurityActions.getDeclaredField(HystrixCircuitBreaker.Factory.class, "circuitBreakersByCommand");
            SecurityActions.setAccessible(field);
            this.circuitBreakers = (ConcurrentHashMap<String, HystrixCircuitBreaker>) field.get(null);
        } catch (Exception e) {
            throw new IllegalStateException("Could not obtain reference to com.netflix.hystrix.HystrixCircuitBreaker.Factory.circuitBreakersByCommand", e);
        }
    }

    @AroundInvoke
    public Object interceptCommand(InvocationContext invocationContext) throws Exception {

        Method method = invocationContext.getMethod();
        Class<?> beanClass = interceptedBean != null ? interceptedBean.getBeanClass() : invocationContext.getTarget().getClass();

        CommandMetadata metadata = commandMetadataCache.computeIfAbsent(method, k -> new CommandMetadata(beanClass, method));
        FaultToleranceOperation operation = metadata.operation;

        if (!operation.isLegitimate()) {
            // HystrixCommandBinding is present but no FT annotation is used
            return invocationContext.proceed();
        }

        ExecutionContextWithInvocationContext ctx = new ExecutionContextWithInvocationContext(invocationContext);
        LOGGER.tracef("FT operation intercepted: %s", method);


        RetryContext retryContext = nonFallBackEnable && operation.hasRetry() ? new RetryContext(operation.getRetry()) : null;
        SynchronousCircuitBreaker syncCircuitBreaker = getSynchronousCircuitBreaker(metadata);

        Cancelator cancelator = new Cancelator(retryContext);

        Function<Supplier<Object>, SimpleCommand> commandFactory =
                (fallback) -> {
                    SimpleCommand simpleCommand = new SimpleCommand(metadata.setter, ctx, fallback, operation, listenersProvider.getCommandListeners(), retryContext);
                    cancelator.setCommand(simpleCommand);
                    return simpleCommand;
                };

        if (operation.isAsync()) {
            LOGGER.debugf("Queue up command for async execution: %s", operation);
            RetryContext retryContextForOneExecution = operation.returnsCompletionStage() ? null : retryContext;
            Callable callable = () -> executeCommand(commandFactory, retryContextForOneExecution, metadata, ctx, syncCircuitBreaker);
            if (operation.returnsCompletionStage()) {
                HystrixObservableCommand command = CompositeObservableCommand.create(
                        (Callable<? extends CompletionStage<?>>) callable,
                        operation,
                        retryContext,
                        ctx,
                        metricsCollectorFactory.isMetricsEnabled() ? metricsCollectorFactory.getRegistry() : null
                );
                return new ObservableCompletableFuture<>(command.observe(), retryContext, method, syncCircuitBreaker);
            } else {
               Future future = CompositeCommand.createAndQueue(
                       callable,
                        operation,
                        ctx,
                        metricsCollectorFactory.isMetricsEnabled() ? metricsCollectorFactory.getRegistry() : null
                );

                return new AsyncFuture(future, cancelator);
            }
        } else {
            LOGGER.debugf("Sync execution: %s]", operation);
            return executeCommand(commandFactory, retryContext, metadata, ctx, syncCircuitBreaker);
        }
    }

    private Object executeCommand(Function<Supplier<Object>, SimpleCommand> commandFactory, RetryContext retryContext, CommandMetadata metadata,
            ExecutionContextWithInvocationContext ctx, SynchronousCircuitBreaker syncCircuitBreaker) throws Exception {

        MetricsCollector metricsCollector = metricsCollectorFactory.createCollector(metadata.operation, retryContext, metadata.poolKey);
        metricsCollector.init(syncCircuitBreaker);

        while (true) {
            if (retryContext != null) {
                LOGGER.debugf("Executing %s with %s", metadata.operation, retryContext);
            }

            SimpleCommand command = commandFactory.apply(metadata.getFallback(ctx));

            metricsCollector.beforeExecute(command);

            try {
                Object res = command.execute();
                if (syncCircuitBreaker != null) {
                    if (command.isFailedExecution() && syncCircuitBreaker.failsOn(command.getFailedExecutionException())) {
                        // this branch is probably never taken...
                        syncCircuitBreaker.executionFailed();
                    } else {
                        syncCircuitBreaker.executionSucceeded();
                    }
                }
                metricsCollector.afterSuccess(command);
                return res;
            } catch (HystrixRuntimeException e) {
                metricsCollector.onError(command, e);
                Exception res = processHystrixRuntimeException(e, retryContext, metadata.operation.getMethod(), syncCircuitBreaker);
                metricsCollector.onProcessedError(command, res);
                if (res != null) {
                    throw res;
                }
            } finally {
                metricsCollector.afterExecute(command);
            }
        }
    }

    private static Exception processHystrixRuntimeException(HystrixRuntimeException e, RetryContext retryContext, Method method,
            SynchronousCircuitBreaker syncCircuitBreaker) {

        FailureType failureType = e.getFailureType();
        LOGGER.tracef("Hystrix runtime failure [%s] with cause %s when invoking %s", failureType, e.getCause(), method);

        // See also SWARM-1933
        Throwable fallbackException = e.getFallbackException();
        if (fallbackException instanceof FailureNotHandledException) {
            // Command failed but the circuit breaker should not be used at all
            FailureNotHandledException failureNotHandledException = (FailureNotHandledException) fallbackException;
            return (Exception) failureNotHandledException.getCause();
        }

        if (syncCircuitBreaker != null) {
            if (syncCircuitBreaker.failsOn(getCause(e))) {
                syncCircuitBreaker.executionFailed();
            } else {
                syncCircuitBreaker.executionSucceeded();
            }
        }

        switch (failureType) {
            case TIMEOUT:
                // Note that TimeoutException should be counted as a failure, even if the command execution completes normally
                TimeoutException timeoutException = new TimeoutException(e);
                if (retryContext != null && retryContext.shouldRetry()) {
                    return retryContext.nextRetry(timeoutException);
                }
                return timeoutException;
            case SHORTCIRCUIT:
                CircuitBreakerOpenException circuitBreakerOpenException = new CircuitBreakerOpenException(method.getName());
                if (retryContext != null && retryContext.shouldRetry()) {
                    return retryContext.nextRetry(circuitBreakerOpenException);
                }
                return circuitBreakerOpenException;
            case REJECTED_THREAD_EXECUTION:
            case REJECTED_SEMAPHORE_EXECUTION:
            case REJECTED_SEMAPHORE_FALLBACK:
                BulkheadException bulkheadException = new BulkheadException(e);
                if (retryContext != null && retryContext.shouldRetry()) {
                    return retryContext.nextRetry(bulkheadException);
                }
                return bulkheadException;
            case COMMAND_EXCEPTION:
                if (retryContext != null && retryContext.shouldRetry()) {
                    return retryContext.nextRetry(getRetryCause(e));
                }
            default:
                return getCause(e);
        }
    }

    private static Throwable getRetryCause(HystrixRuntimeException e) {
        if (e.getCause() instanceof Exception) {
            // For some reason Hystrix also wraps errors
            return e.getCause().getCause() instanceof Error ? e.getCause().getCause() : e.getCause();
        }
        return e;
    }

    private static Exception getCause(HystrixRuntimeException e) {
        return (e.getCause() instanceof Exception) ? (Exception) e.getCause() : e;
    }

    private SynchronousCircuitBreaker getSynchronousCircuitBreaker(CommandMetadata metadata) {
        if (nonFallBackEnable && syncCircuitBreakerEnabled && metadata.hasCircuitBreaker()) {
            HystrixCircuitBreaker circuitBreaker = circuitBreakers.computeIfAbsent(metadata.commandKey.name(),
                    (key) -> new SynchronousCircuitBreaker(metadata.operation.getCircuitBreaker()));
            if (circuitBreaker instanceof SynchronousCircuitBreaker) {
                return (SynchronousCircuitBreaker) circuitBreaker;
            }
            throw new IllegalStateException("Cached circuit breaker does not extend SynchronousCircuitBreaker");
        }
        return null;
    }

    private static IllegalStateException errorProcessingHystrixRuntimeException(HystrixRuntimeException e) {
        return new IllegalStateException("Error during processing hystrix runtime exception", e);
    }

    private Setter initCommandSetter(HystrixCommandKey commandKey, HystrixThreadPoolKey poolKey, Method method, FaultToleranceOperation operation) {
        HystrixCommandProperties.Setter propertiesSetter = HystrixCommandProperties.Setter();

        // Async and timeout operations use THREAD isolation strategy
        if (operation.isAsync() || operation.hasTimeout()) {
            propertiesSetter.withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD);
        } else {
            propertiesSetter.withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE);
        }

        if (nonFallBackEnable && operation.hasTimeout()) {
            Long value = Duration.of(operation.getTimeout().get(TimeoutConfig.VALUE), operation.getTimeout().get(TimeoutConfig.UNIT)).toMillis();
            if (value > Integer.MAX_VALUE) {
                LOGGER.warnf("Max supported value for @Timeout.value() is %s", Integer.MAX_VALUE);
                value = Long.valueOf(Integer.MAX_VALUE);
            }
            propertiesSetter.withExecutionTimeoutInMilliseconds(value.intValue());
            propertiesSetter.withExecutionIsolationThreadInterruptOnTimeout(true);
        } else {
            propertiesSetter.withExecutionTimeoutEnabled(false);
        }

        if (nonFallBackEnable && operation.hasCircuitBreaker()) {
            propertiesSetter.withCircuitBreakerEnabled(true)
                    .withCircuitBreakerRequestVolumeThreshold(operation.getCircuitBreaker().get(CircuitBreakerConfig.REQUEST_VOLUME_THRESHOLD))
                    .withCircuitBreakerErrorThresholdPercentage(
                            new Double((Double) operation.getCircuitBreaker().get(CircuitBreakerConfig.FAILURE_RATIO) * 100).intValue())
                    .withCircuitBreakerSleepWindowInMilliseconds((int) Duration.of(operation.getCircuitBreaker().get(CircuitBreakerConfig.DELAY),
                            operation.getCircuitBreaker().get(CircuitBreakerConfig.DELAY_UNIT)).toMillis());
        } else {
            propertiesSetter.withCircuitBreakerEnabled(false);
        }

        Setter setter = Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("DefaultCommandGroup"))
                // Each method must have a unique command key
                .andCommandKey(commandKey).andCommandPropertiesDefaults(propertiesSetter).andThreadPoolKey(poolKey);

        if (nonFallBackEnable && operation.hasBulkhead()) {
            BulkheadConfig bulkhead = operation.getBulkhead();
            if (operation.isAsync()) {
                HystrixThreadPoolProperties.Setter threadPoolSetter = HystrixThreadPoolProperties.Setter();
                threadPoolSetter.withAllowMaximumSizeToDivergeFromCoreSize(false).withCoreSize(bulkhead.get(BulkheadConfig.VALUE))
                        .withMaximumSize(bulkhead.get(BulkheadConfig.VALUE)).withMaxQueueSize(bulkhead.get(BulkheadConfig.WAITING_TASK_QUEUE))
                        .withQueueSizeRejectionThreshold(bulkhead.get(BulkheadConfig.WAITING_TASK_QUEUE));
                setter.andThreadPoolPropertiesDefaults(threadPoolSetter);
            } else {
                // If used without @Asynchronous, the semaphore isolation approach must be used
                propertiesSetter.withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE);
                propertiesSetter.withExecutionIsolationSemaphoreMaxConcurrentRequests(bulkhead.get(BulkheadConfig.VALUE));
                propertiesSetter.withExecutionIsolationThreadInterruptOnFutureCancel(true);
            }
        }
        return setter;
    }

    private class CommandMetadata {

        private final Setter setter;

        private final HystrixCommandKey commandKey;

        private final HystrixThreadPoolKey poolKey;

        private final Method fallbackMethod;

        private final FaultToleranceOperation operation;

        CommandMetadata(Class<?> beanClass, Method method) {
            operation = faultToleranceOperationProvider.get(beanClass, method);
            // Initialize Hystrix command setter
            commandKey = HystrixCommandKey.Factory.asKey(SimpleCommand.getCommandKey(method));

            if (nonFallBackEnable && operation.hasBulkhead() && operation.isAsync()) {
                // Each bulkhead policy needs a dedicated thread pool
                poolKey = HystrixThreadPoolKey.Factory.asKey(commandKey.name());
            } else {
                poolKey = HystrixThreadPoolKey.Factory.asKey("DefaultCommandGroup");
            }

            setter = initCommandSetter(commandKey, poolKey, method, operation);

            if (operation.hasFallback()) {
                FallbackConfig fallbackConfig = operation.getFallback();
                if (!fallbackConfig.get(FallbackConfig.VALUE).equals(Fallback.DEFAULT.class)) {
                    fallbackMethod = null;
                } else {
                    String fallbackMethodName = fallbackConfig.get(FallbackConfig.FALLBACK_METHOD);
                    if (!"".equals(fallbackMethodName)) {
                        try {
                            fallbackMethod = SecurityActions.getDeclaredMethod(beanClass, method.getDeclaringClass(), fallbackMethodName, method.getGenericParameterTypes());
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
            } else {
                fallbackMethod = null;
            }
        }

        boolean hasCircuitBreaker() {
            return operation.hasCircuitBreaker();
        }

        Supplier<Object> getFallback(ExecutionContextWithInvocationContext ctx) {
            Supplier<Object> fallback = null;
            if (fallbackMethod != null) {
                fallback = () -> {
                    try {
                        if (fallbackMethod.isDefault()) {
                            // Workaround for default methods (used e.g. in MP Rest Client)
                            return DefaultMethodFallbackProvider.getFallback(fallbackMethod, ctx);
                        } else {
                            return fallbackMethod.invoke(ctx.getTarget(), ctx.getParameters());
                        }
                    } catch (Throwable e) {
                        throw new FaultToleranceException("Error during fallback method invocation", e);
                    }
                };
            } else {
                FallbackHandler<?> fallbackHandler = fallbackHandlerProvider.get(operation);
                if (fallbackHandler != null) {
                    fallback = () -> fallbackHandler.handle(ctx);
                }
            }
            return fallback;
        }
    }

    static class ObservableCompletableFuture<T> extends CompletableFuture<T> {
        private final Subscription subscription;
        private final RetryContext retryContext;
        private final Method method;
        private final SynchronousCircuitBreaker syncCircuitBreaker;

        ObservableCompletableFuture(Observable<T> observable, RetryContext retryContext, Method method, SynchronousCircuitBreaker syncCircuitBreaker) {
            this.retryContext = retryContext;
            this.method = method;
            this.syncCircuitBreaker = syncCircuitBreaker;
            subscription = observable.single()
                    .subscribe(this::complete, this::completeExceptionally);
        }

        @Override
        public boolean completeExceptionally(Throwable ex) {
            if (ex instanceof HystrixRuntimeException) {
                return super.completeExceptionally(processHystrixRuntimeException((HystrixRuntimeException) ex, retryContext, method, syncCircuitBreaker));
            }
            return super.completeExceptionally(ex);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            this.subscription.unsubscribe();
            return super.cancel(mayInterruptIfRunning);
        }
    }

    static class AsyncFuture implements Future<Object> {

        private final Future<Object> delegate;
        private final Cancelator cancelator;

        public AsyncFuture(Future<Object> delegate, Cancelator cancelator) {
            this.delegate = delegate;
            this.cancelator = cancelator;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelator.cancel(mayInterruptIfRunning);
            return delegate.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return delegate.isCancelled();
        }

        @Override
        public boolean isDone() {
            return delegate.isDone();
        }

        @Override
        public Object get() throws InterruptedException, ExecutionException {
            Future<Object> future;
            try {
                future = unwrapFuture(delegate.get());
            } catch (ExecutionException e) {
                throw unwrapExecutionException(e);
            }
            try {
                return logResult(future, future.get());
            } catch (ExecutionException e) {
                // Rethrow if completed exceptionally
                throw e;
            } catch (Exception e) {
                throw unableToUnwrap(future);
            }
        }

        @Override
        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {
            Future<Object> future;
            try {
                future = unwrapFuture(delegate.get());
            } catch (ExecutionException e) {
                throw unwrapExecutionException(e);
            }
            try {
                return logResult(future, future.get(timeout, unit));
            } catch (ExecutionException e) {
                // Rethrow if completed exceptionally
                throw e;
            } catch (Exception e) {
                throw unableToUnwrap(future);
            }
        }

        @SuppressWarnings("unchecked")
        private Future<Object> unwrapFuture(Object futureObject) {
            if (futureObject instanceof Future) {
                return (Future<Object>) futureObject;
            } else {
                throw new IllegalStateException("A result of an @Asynchronous call must be Future: " + futureObject);
            }
        }

        private Object logResult(Future<Object> future, Object unwrapped) {
            LOGGER.tracef("Unwrapped async result from %s: %s", future, unwrapped);
            return unwrapped;
        }
    }

    private static ExecutionException unwrapExecutionException(ExecutionException e) throws ExecutionException {
        if (e.getCause() instanceof HystrixRuntimeException) {
            Exception res;
            HystrixRuntimeException hystrixRuntimeException = (HystrixRuntimeException) e.getCause();
            if (FailureType.COMMAND_EXCEPTION.equals(hystrixRuntimeException.getFailureType())) {
                res = getCause(hystrixRuntimeException);
            } else {
                res = errorProcessingHystrixRuntimeException(hystrixRuntimeException);
            }
            return new ExecutionException(res);
        }
        return e;
    }

    private static IllegalStateException unableToUnwrap(Future<Object> future) {
        return new IllegalStateException("Unable to get the result of: " + future);
    }

    private class Cancelator {
        private final RetryContext retryContext;
        private SimpleCommand command;
        private AtomicBoolean canceled = new AtomicBoolean(false);

        Cancelator(RetryContext retryContext) {
            this.retryContext = retryContext;
        }

        void setCommand(SimpleCommand command) {
            this.command = command;
            if (canceled.get()) {
                cancel(true);
            }
        }

        void cancel(boolean mayInterruptIfRunning) {
            if (retryContext != null) {
                retryContext.cancel();
            }
            command.cancel(mayInterruptIfRunning);
        }
    }
}
