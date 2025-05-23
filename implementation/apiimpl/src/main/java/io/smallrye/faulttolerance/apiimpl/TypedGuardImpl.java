package io.smallrye.faulttolerance.apiimpl;

import static io.smallrye.faulttolerance.apiimpl.GuardCommon.createExceptionDecision;
import static io.smallrye.faulttolerance.apiimpl.GuardCommon.createResultDecision;
import static io.smallrye.faulttolerance.core.Invocation.invocation;
import static io.smallrye.faulttolerance.core.util.Durations.timeInMillis;
import static io.smallrye.faulttolerance.core.util.Preconditions.check;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;
import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.faulttolerance.api.CustomBackoffStrategy;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import io.smallrye.faulttolerance.api.FibonacciBackoff;
import io.smallrye.faulttolerance.api.RateLimitType;
import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.basicconfig.BasicFaultToleranceOperation;
import io.smallrye.faulttolerance.core.FaultToleranceContext;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.async.RememberEventLoop;
import io.smallrye.faulttolerance.core.async.SyncAsyncSplit;
import io.smallrye.faulttolerance.core.async.ThreadOffload;
import io.smallrye.faulttolerance.core.bulkhead.Bulkhead;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreaker;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerEvents;
import io.smallrye.faulttolerance.core.fallback.Fallback;
import io.smallrye.faulttolerance.core.fallback.FallbackFunction;
import io.smallrye.faulttolerance.core.invocation.AsyncSupport;
import io.smallrye.faulttolerance.core.invocation.ConstantInvoker;
import io.smallrye.faulttolerance.core.metrics.DelegatingMetricsCollector;
import io.smallrye.faulttolerance.core.metrics.MeteredOperation;
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
import io.smallrye.faulttolerance.core.timeout.Timeout;
import io.smallrye.faulttolerance.core.util.ExceptionDecision;

// V = value type, e.g. String
// T = result type, e.g. String or CompletionStage<String> or Uni<String>
//
// in synchronous scenario, V = T
// in asynchronous scenario, T is an async type that eventually produces V
public final class TypedGuardImpl<V, T> implements TypedGuard<T> {
    private final FaultToleranceStrategy<V> strategy;
    private final AsyncSupport<V, T> asyncSupport;
    private final EventHandlers eventHandlers;

    // Circuit breakers created using the programmatic API are registered with `CircuitBreakerMaintenance`
    // in two phases:
    //
    // 1. The name is registered eagerly, during `BuilderImpl.build()`, so that `CircuitBreakerMaintenance` methods
    //    may be called immediately after building the `TypedGuard` instance. A single name may be registered
    //    multiple times.
    // 2. The circuit breaker instance is registered lazily, during `BuilderImpl.buildStrategy()`, so that
    //    a `TypedGuard` instance that is created but never used doesn't prevent an actually useful
    //    circuit breaker from being registered later. Only one circuit breaker may be registered
    //    for given name.
    //
    // Lazy registration of circuit breakers exists to allow normal-scoped CDI beans to declare `TypedGuard`
    // fields that are assigned inline (which effectively means in a constructor). Normal-scoped beans always
    // have a client proxy, whose class inherits from the bean class and calls the superclass's zero-parameter
    // constructor. This leads to the client proxy instance also having an instance of `TypedGuard`,
    // but that instance is never used. The useful `TypedGuard` instance is held by the actual bean instance,
    // which is created lazily, on the first method invocation on the client proxy.

    TypedGuardImpl(FaultToleranceStrategy<V> strategy, AsyncSupport<V, T> asyncSupport, EventHandlers eventHandlers) {
        this.strategy = strategy;
        this.asyncSupport = asyncSupport;
        this.eventHandlers = eventHandlers;
    }

    @Override
    public T call(Callable<T> action) throws Exception {
        return guard(action);
    }

    @Override
    public T get(Supplier<T> action) {
        try {
            return guard(action::get);
        } catch (Exception e) {
            throw sneakyThrow(e);
        }
    }

    private T guard(Callable<T> action) throws Exception {
        AsyncInvocation<V, T> asyncInvocation = GuardCommon.asyncInvocation(action, asyncSupport);
        return GuardCommon.guard(action, strategy, asyncInvocation, eventHandlers, null);
    }

    public T guard(Callable<T> action, AsyncInvocation<V, T> asyncInvocation,
            Consumer<FaultToleranceContext<?>> contextModifier) throws Exception {
        return GuardCommon.guard(action, strategy, asyncInvocation, eventHandlers, contextModifier);
    }

    public static class BuilderImpl<V, T> implements TypedGuard.Builder<T> {
        private final BuilderEagerDependencies eagerDependencies;
        private final Supplier<BuilderLazyDependencies> lazyDependencies;
        private final AsyncSupport<V, T> asyncSupport;

        private String description;
        private boolean descriptionSet;
        private BulkheadBuilderImpl<V, T> bulkheadBuilder;
        private CircuitBreakerBuilderImpl<V, T> circuitBreakerBuilder;
        private FallbackBuilderImpl<V, T> fallbackBuilder;
        private RateLimitBuilderImpl<V, T> rateLimitBuilder;
        private RetryBuilderImpl<V, T> retryBuilder;
        private TimeoutBuilderImpl<V, T> timeoutBuilder;
        private boolean offloadToAnotherThread;
        private Executor offloadExecutor;

        public BuilderImpl(BuilderEagerDependencies eagerDependencies, Supplier<BuilderLazyDependencies> lazyDependencies,
                Type valueType) {
            this.eagerDependencies = eagerDependencies;
            this.lazyDependencies = lazyDependencies;
            this.asyncSupport = GuardCommon.asyncSupport(valueType);

            this.description = UUID.randomUUID().toString();
            this.descriptionSet = false;
        }

        @Override
        public Builder<T> withDescription(String value) {
            this.description = checkNotNull(value, "Description must be set");
            this.descriptionSet = true;
            return this;
        }

        @Override
        public BulkheadBuilder<T> withBulkhead() {
            return new BulkheadBuilderImpl<>(this);
        }

        @Override
        public CircuitBreakerBuilder<T> withCircuitBreaker() {
            return new CircuitBreakerBuilderImpl<>(this);
        }

        @Override
        public FallbackBuilder<T> withFallback() {
            return new FallbackBuilderImpl<>(this);
        }

        @Override
        public RateLimitBuilder<T> withRateLimit() {
            return new RateLimitBuilderImpl<>(this);
        }

        @Override
        public RetryBuilder<T> withRetry() {
            return new RetryBuilderImpl<>(this);
        }

        @Override
        public TimeoutBuilder<T> withTimeout() {
            return new TimeoutBuilderImpl<>(this);
        }

        @Override
        public Builder<T> withThreadOffload(boolean value) {
            this.offloadToAnotherThread = value;
            return this;
        }

        @Override
        public Builder<T> withThreadOffloadExecutor(Executor executor) {
            this.offloadExecutor = checkNotNull(executor, "Thread offload executor must be set");
            return this;
        }

        @Override
        public TypedGuard<T> build() {
            eagerInitialization();
            return new LazyTypedGuard<>(cfg -> new TypedGuardImpl<>(
                    buildStrategy(cfg, lazyDependencies.get()), asyncSupport, buildEventHandlers()));
        }

        // must not access lazyDependencies
        final void eagerInitialization() {
            if (circuitBreakerBuilder != null && circuitBreakerBuilder.name != null) {
                eagerDependencies.cbMaintenance().registerName(circuitBreakerBuilder.name);
            }
        }

        final EventHandlers buildEventHandlers() {
            Consumer<CircuitBreakerEvents.StateTransition> cbMaintenanceEventHandler = null;
            if (circuitBreakerBuilder != null && circuitBreakerBuilder.name != null) {
                cbMaintenanceEventHandler = eagerDependencies.cbMaintenance()
                        .stateTransitionEventHandler(circuitBreakerBuilder.name);
            }
            return new EventHandlers(
                    bulkheadBuilder != null ? bulkheadBuilder.onAccepted : null,
                    bulkheadBuilder != null ? bulkheadBuilder.onRejected : null,
                    bulkheadBuilder != null ? bulkheadBuilder.onFinished : null,
                    cbMaintenanceEventHandler,
                    circuitBreakerBuilder != null ? circuitBreakerBuilder.onStateChange : null,
                    circuitBreakerBuilder != null ? circuitBreakerBuilder.onSuccess : null,
                    circuitBreakerBuilder != null ? circuitBreakerBuilder.onFailure : null,
                    circuitBreakerBuilder != null ? circuitBreakerBuilder.onPrevented : null,
                    rateLimitBuilder != null ? rateLimitBuilder.onPermitted : null,
                    rateLimitBuilder != null ? rateLimitBuilder.onRejected : null,
                    retryBuilder != null ? retryBuilder.onRetry : null,
                    retryBuilder != null ? retryBuilder.onSuccess : null,
                    retryBuilder != null ? retryBuilder.onFailure : null,
                    timeoutBuilder != null ? timeoutBuilder.onTimeout : null,
                    timeoutBuilder != null ? timeoutBuilder.onFinished : null);
        }

        final FaultToleranceStrategy<V> buildStrategy(String id, BuilderLazyDependencies lazyDependencies) {
            BasicFaultToleranceOperation op = new BasicFaultToleranceOperation(id, bulkheadBuilder,
                    circuitBreakerBuilder, rateLimitBuilder, retryBuilder, timeoutBuilder,
                    retryBuilder != null ? retryBuilder.exponentialBackoffBuilder : null,
                    retryBuilder != null ? retryBuilder.fibonacciBackoffBuilder : null);
            op.validate();

            FaultToleranceStrategy<V> result = invocation();

            // thread offload is always enabled
            Executor executor = offloadExecutor != null ? offloadExecutor : lazyDependencies.asyncExecutor();
            result = new SyncAsyncSplit<>(
                    new ThreadOffload<>(result, executor, offloadToAnotherThread),
                    result);

            if (lazyDependencies.ftEnabled() && op.hasBulkhead()) {
                result = new Bulkhead<>(result, description,
                        op.getBulkhead().value(),
                        op.getBulkhead().waitingTaskQueue(),
                        bulkheadBuilder.syncQueueingEnabled);
            }

            if (lazyDependencies.ftEnabled() && op.hasTimeout()) {
                result = new Timeout<>(result, description,
                        timeInMillis(op.getTimeout().value(), op.getTimeout().unit()),
                        lazyDependencies.timer());
            }

            if (lazyDependencies.ftEnabled() && op.hasRateLimit()) {
                result = new RateLimit<>(result, description,
                        op.getRateLimit().value(),
                        timeInMillis(op.getRateLimit().window(), op.getRateLimit().windowUnit()),
                        timeInMillis(op.getRateLimit().minSpacing(), op.getRateLimit().minSpacingUnit()),
                        op.getRateLimit().type(),
                        SystemStopwatch.INSTANCE);
            }

            if (lazyDependencies.ftEnabled() && op.hasCircuitBreaker()) {
                result = new CircuitBreaker<>(result, description,
                        createExceptionDecision(op.getCircuitBreaker().skipOn(), op.getCircuitBreaker().failOn(),
                                circuitBreakerBuilder.whenPredicate),
                        timeInMillis(op.getCircuitBreaker().delay(), op.getCircuitBreaker().delayUnit()),
                        op.getCircuitBreaker().requestVolumeThreshold(),
                        op.getCircuitBreaker().failureRatio(),
                        op.getCircuitBreaker().successThreshold(),
                        SystemStopwatch.INSTANCE,
                        lazyDependencies.timer());

                if (circuitBreakerBuilder.name != null) {
                    CircuitBreaker<?> circuitBreaker = (CircuitBreaker<?>) result;
                    eagerDependencies.cbMaintenance().register(circuitBreakerBuilder.name, circuitBreaker);
                }
            }

            if (lazyDependencies.ftEnabled() && op.hasRetry()) {
                Supplier<BackOff> backoff = prepareRetryBackoff(op, retryBuilder);
                Consumer<Throwable> beforeRetryAction = retryBuilder.beforeRetry;

                result = new Retry<>(result, description,
                        createResultDecision(retryBuilder.whenResultPredicate),
                        createExceptionDecision(op.getRetry().abortOn(), op.getRetry().retryOn(),
                                retryBuilder.whenExceptionPredicate),
                        op.getRetry().maxRetries(),
                        timeInMillis(op.getRetry().maxDuration(), op.getRetry().durationUnit()),
                        () -> new ThreadSleepDelay(backoff.get()),
                        () -> new TimerDelay(backoff.get(), lazyDependencies.timer()),
                        SystemStopwatch.INSTANCE,
                        beforeRetryAction != null ? ctx -> beforeRetryAction.accept(ctx.failure) : null);
            }

            // fallback is always enabled
            FallbackFunction<V> fallbackFunction = FallbackFunction.ignore();
            ExceptionDecision exceptionDecision = ExceptionDecision.IGNORE;
            if (fallbackBuilder != null) {
                if (asyncSupport != null) {
                    fallbackFunction = ctx -> {
                        try {
                            return asyncSupport.toFuture(ConstantInvoker.of(
                                    fallbackBuilder.handler.apply(ctx.failure)));
                        } catch (Exception e) {
                            return Future.ofError(e);
                        }
                    };
                } else {
                    fallbackFunction = ctx -> {
                        return Future.from(() -> (V) fallbackBuilder.handler.apply(ctx.failure));
                    };
                }

                exceptionDecision = createExceptionDecision(fallbackBuilder.skipOn, fallbackBuilder.applyOn,
                        fallbackBuilder.whenPredicate);
            }
            result = new Fallback<>(result, description, fallbackFunction, exceptionDecision);

            MetricsProvider metricsProvider = lazyDependencies.metricsProvider();
            if (metricsProvider.isEnabled()) {
                MeteredOperation defaultOperation = buildMeteredOperation();
                result = new DelegatingMetricsCollector<>(result, metricsProvider, defaultOperation);
            }

            // thread offload is always enabled
            result = new SyncAsyncSplit<>(
                    new RememberEventLoop<>(result, lazyDependencies.eventLoop(), offloadToAnotherThread),
                    result);

            return result;
        }

        private MeteredOperation buildMeteredOperation() {
            return new BasicMeteredOperationImpl(descriptionSet, description, asyncSupport != null, bulkheadBuilder != null,
                    circuitBreakerBuilder != null, false, rateLimitBuilder != null,
                    retryBuilder != null, timeoutBuilder != null);
        }

        private static Supplier<BackOff> prepareRetryBackoff(BasicFaultToleranceOperation op,
                RetryBuilderImpl<?, ?> retryBuilder) {
            long delayMs = timeInMillis(op.getRetry().delay(), op.getRetry().delayUnit());
            long jitterMs = timeInMillis(op.getRetry().jitter(), op.getRetry().jitterDelayUnit());
            Jitter jitter = jitterMs == 0 ? Jitter.ZERO : new RandomJitter(jitterMs);

            if (op.hasExponentialBackoff()) {
                int factor = op.getExponentialBackoff().factor();
                long maxDelayMs = timeInMillis(op.getExponentialBackoff().maxDelay(),
                        op.getExponentialBackoff().maxDelayUnit());
                return () -> new ExponentialBackOff(delayMs, factor, jitter, maxDelayMs);
            } else if (op.hasFibonacciBackoff()) {
                long maxDelayMs = timeInMillis(op.getFibonacciBackoff().maxDelay(), op.getFibonacciBackoff().maxDelayUnit());
                return () -> new FibonacciBackOff(delayMs, jitter, maxDelayMs);
            } else if (retryBuilder.customBackoffBuilder != null) {
                Supplier<CustomBackoffStrategy> strategy = retryBuilder.customBackoffBuilder.strategy;
                return () -> {
                    CustomBackoffStrategy instance = strategy.get();
                    instance.init(delayMs);
                    return new CustomBackOff(instance::nextDelayInMillis);
                };
            } else {
                return () -> new ConstantBackOff(delayMs, jitter);
            }
        }

        static class BulkheadBuilderImpl<V, T> implements BulkheadBuilder<T>,
                Supplier<org.eclipse.microprofile.faulttolerance.Bulkhead> {
            private final BuilderImpl<V, T> parent;

            private int limit = 10;
            private int queueSize = 10;
            private boolean syncQueueingEnabled;

            private Runnable onAccepted;
            private Runnable onRejected;
            private Runnable onFinished;

            BulkheadBuilderImpl(BuilderImpl<V, T> parent) {
                this.parent = parent;
            }

            @Override
            public BulkheadBuilder<T> limit(int value) {
                this.limit = check(value, value >= 1, "Limit must be >= 1");
                return this;
            }

            @Override
            public BulkheadBuilder<T> queueSize(int value) {
                this.queueSize = check(value, value >= 1, "Queue size must be >= 1");
                return this;
            }

            @Override
            public BulkheadBuilder<T> enableSynchronousQueueing() {
                this.syncQueueingEnabled = true;
                return this;
            }

            @Override
            public BulkheadBuilder<T> onAccepted(Runnable callback) {
                this.onAccepted = checkNotNull(callback, "Accepted callback must be set");
                return this;
            }

            @Override
            public BulkheadBuilder<T> onRejected(Runnable callback) {
                this.onRejected = checkNotNull(callback, "Rejected callback must be set");
                return this;
            }

            @Override
            public BulkheadBuilder<T> onFinished(Runnable callback) {
                this.onFinished = checkNotNull(callback, "Finished callback must be set");
                return this;
            }

            @Override
            public Builder<T> done() {
                try {
                    Math.addExact(limit, queueSize);
                } catch (ArithmeticException e) {
                    throw new IllegalStateException("Bulkhead capacity overflow, " + limit + " + " + queueSize
                            + " = " + (limit + queueSize));
                }

                parent.bulkheadBuilder = this;
                return parent;
            }

            @Override
            public org.eclipse.microprofile.faulttolerance.Bulkhead get() {
                return new org.eclipse.microprofile.faulttolerance.Bulkhead() {
                    @Override
                    public int value() {
                        return limit;
                    }

                    @Override
                    public int waitingTaskQueue() {
                        return queueSize;
                    }

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return org.eclipse.microprofile.faulttolerance.Bulkhead.class;
                    }
                };
            }
        }

        static class CircuitBreakerBuilderImpl<V, T> implements CircuitBreakerBuilder<T>,
                Supplier<org.eclipse.microprofile.faulttolerance.CircuitBreaker> {
            private final BuilderImpl<V, T> parent;

            private Class<? extends Throwable>[] failOn = new Class[] { Throwable.class };
            private Class<? extends Throwable>[] skipOn = new Class[] {};
            private boolean setBasedExceptionDecisionDefined = false;
            private Predicate<Throwable> whenPredicate;
            private long delay = 5000;
            private ChronoUnit delayUnit = ChronoUnit.MILLIS;
            private int requestVolumeThreshold = 20;
            private double failureRatio = 0.5;
            private int successThreshold = 1;

            private String name; // unnamed by default

            private Consumer<CircuitBreakerState> onStateChange;
            private Runnable onSuccess;
            private Runnable onFailure;
            private Runnable onPrevented;

            CircuitBreakerBuilderImpl(BuilderImpl<V, T> parent) {
                this.parent = parent;
            }

            @Override
            public CircuitBreakerBuilder<T> failOn(Collection<Class<? extends Throwable>> value) {
                this.failOn = checkNotNull(value, "Exceptions considered failure must be set").toArray(new Class[0]);
                this.setBasedExceptionDecisionDefined = true;
                return this;
            }

            @Override
            public CircuitBreakerBuilder<T> failOn(Class<? extends Throwable> value) {
                this.failOn = new Class[] { checkNotNull(value, "Exception considered failure must be set") };
                this.setBasedExceptionDecisionDefined = true;
                return this;
            }

            @Override
            public CircuitBreakerBuilder<T> skipOn(Collection<Class<? extends Throwable>> value) {
                this.skipOn = checkNotNull(value, "Exceptions considered success must be set").toArray(new Class[0]);
                this.setBasedExceptionDecisionDefined = true;
                return this;
            }

            @Override
            public CircuitBreakerBuilder<T> skipOn(Class<? extends Throwable> value) {
                this.skipOn = new Class[] { checkNotNull(value, "Exception considered success must be set") };
                this.setBasedExceptionDecisionDefined = true;
                return this;
            }

            @Override
            public CircuitBreakerBuilder<T> when(Predicate<Throwable> value) {
                this.whenPredicate = checkNotNull(value, "Exception predicate must be set");
                return this;
            }

            @Override
            public CircuitBreakerBuilder<T> delay(long value, ChronoUnit unit) {
                check(value, value >= 0, "Delay must be >= 0");
                checkNotNull(unit, "Delay unit must be set");

                this.delay = value;
                this.delayUnit = unit;
                return this;
            }

            @Override
            public CircuitBreakerBuilder<T> requestVolumeThreshold(int value) {
                this.requestVolumeThreshold = check(value, value >= 1, "Request volume threshold must be >= 1");
                return this;
            }

            @Override
            public CircuitBreakerBuilder<T> failureRatio(double value) {
                this.failureRatio = check(value, value >= 0 && value <= 1, "Failure ratio must be >= 0 and <= 1");
                return this;
            }

            @Override
            public CircuitBreakerBuilder<T> successThreshold(int value) {
                this.successThreshold = check(value, value >= 1, "Success threshold must be >= 1");
                return this;
            }

            @Override
            public CircuitBreakerBuilder<T> name(String value) {
                this.name = checkNotNull(value, "Circuit breaker name must be set");
                return this;
            }

            @Override
            public CircuitBreakerBuilder<T> onStateChange(Consumer<CircuitBreakerState> callback) {
                this.onStateChange = checkNotNull(callback, "On state change callback must be set");
                return this;
            }

            @Override
            public CircuitBreakerBuilder<T> onSuccess(Runnable callback) {
                this.onSuccess = checkNotNull(callback, "On success callback must be set");
                return this;
            }

            @Override
            public CircuitBreakerBuilder<T> onFailure(Runnable callback) {
                this.onFailure = checkNotNull(callback, "On failure callback must be set");
                return this;
            }

            @Override
            public CircuitBreakerBuilder<T> onPrevented(Runnable callback) {
                this.onPrevented = checkNotNull(callback, "On prevented callback must be set");
                return this;
            }

            @Override
            public Builder<T> done() {
                if (whenPredicate != null && setBasedExceptionDecisionDefined) {
                    throw new IllegalStateException("The when() method may not be combined with failOn() / skipOn()");
                }

                parent.circuitBreakerBuilder = this;
                return parent;
            }

            @Override
            public org.eclipse.microprofile.faulttolerance.CircuitBreaker get() {
                return new org.eclipse.microprofile.faulttolerance.CircuitBreaker() {
                    @Override
                    public Class<? extends Throwable>[] failOn() {
                        return failOn;
                    }

                    @Override
                    public Class<? extends Throwable>[] skipOn() {
                        return skipOn;
                    }

                    @Override
                    public long delay() {
                        return delay;
                    }

                    @Override
                    public ChronoUnit delayUnit() {
                        return delayUnit;
                    }

                    @Override
                    public int requestVolumeThreshold() {
                        return requestVolumeThreshold;
                    }

                    @Override
                    public double failureRatio() {
                        return failureRatio;
                    }

                    @Override
                    public int successThreshold() {
                        return successThreshold;
                    }

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return org.eclipse.microprofile.faulttolerance.CircuitBreaker.class;
                    }
                };
            }
        }

        static class FallbackBuilderImpl<V, T> implements FallbackBuilder<T> {
            private final BuilderImpl<V, T> parent;

            private Function<Throwable, T> handler;
            private Class<? extends Throwable>[] applyOn = new Class[] { Throwable.class };
            private Class<? extends Throwable>[] skipOn = new Class[] {};
            private boolean setBasedExceptionDecisionDefined = false;
            private Predicate<Throwable> whenPredicate;

            FallbackBuilderImpl(BuilderImpl<V, T> parent) {
                this.parent = parent;
            }

            @Override
            public FallbackBuilder<T> handler(Supplier<T> value) {
                checkNotNull(value, "Fallback handler must be set");
                this.handler = ignored -> value.get();
                return this;
            }

            @Override
            public FallbackBuilder<T> handler(Function<Throwable, T> value) {
                this.handler = checkNotNull(value, "Fallback handler must be set");
                return this;
            }

            @Override
            public FallbackBuilder<T> applyOn(Collection<Class<? extends Throwable>> value) {
                this.applyOn = checkNotNull(value, "Exceptions to apply fallback on must be set").toArray(new Class[0]);
                this.setBasedExceptionDecisionDefined = true;
                return this;
            }

            @Override
            public FallbackBuilder<T> applyOn(Class<? extends Throwable> value) {
                this.applyOn = new Class[] { checkNotNull(value, "Exception to apply fallback on must be set") };
                this.setBasedExceptionDecisionDefined = true;
                return this;
            }

            @Override
            public FallbackBuilder<T> skipOn(Collection<Class<? extends Throwable>> value) {
                this.skipOn = checkNotNull(value, "Exceptions to skip fallback on must be set").toArray(new Class[0]);
                this.setBasedExceptionDecisionDefined = true;
                return this;
            }

            @Override
            public FallbackBuilder<T> skipOn(Class<? extends Throwable> value) {
                this.skipOn = new Class[] { checkNotNull(value, "Exception to skip fallback on must be set") };
                this.setBasedExceptionDecisionDefined = true;
                return this;
            }

            @Override
            public FallbackBuilder<T> when(Predicate<Throwable> value) {
                this.whenPredicate = checkNotNull(value, "Exception predicate must be set");
                return this;
            }

            @Override
            public Builder<T> done() {
                checkNotNull(handler, "Fallback handler must be set");

                if (whenPredicate != null && setBasedExceptionDecisionDefined) {
                    throw new IllegalStateException("The when() method may not be combined with applyOn() / skipOn()");
                }

                parent.fallbackBuilder = this;
                return parent;
            }
        }

        static class RateLimitBuilderImpl<V, T> implements RateLimitBuilder<T>,
                Supplier<io.smallrye.faulttolerance.api.RateLimit> {
            private final BuilderImpl<V, T> parent;

            private int maxInvocations = 100;
            private long timeWindow = 1;
            private ChronoUnit timeWindowUnit = ChronoUnit.SECONDS;
            private long minSpacing = 0;
            private ChronoUnit minSpacingUnit = ChronoUnit.SECONDS;
            private RateLimitType type = RateLimitType.FIXED;

            private Runnable onPermitted;
            private Runnable onRejected;

            RateLimitBuilderImpl(BuilderImpl<V, T> parent) {
                this.parent = parent;
            }

            @Override
            public RateLimitBuilder<T> limit(int value) {
                this.maxInvocations = check(value, value >= 1, "Rate limit must be >= 1");
                return this;
            }

            @Override
            public RateLimitBuilder<T> window(long value, ChronoUnit unit) {
                check(value, value >= 1, "Time window length must be >= 1");
                checkNotNull(unit, "Time window length unit must be set");

                this.timeWindow = value;
                this.timeWindowUnit = unit;
                return this;
            }

            @Override
            public RateLimitBuilder<T> minSpacing(long value, ChronoUnit unit) {
                check(value, value >= 0, "Min spacing must be >= 0");
                checkNotNull(unit, "Min spacing unit must be set");

                this.minSpacing = value;
                this.minSpacingUnit = unit;
                return this;
            }

            @Override
            public RateLimitBuilder<T> type(RateLimitType value) {
                this.type = checkNotNull(value, "Time window type must be set");
                return this;
            }

            @Override
            public RateLimitBuilder<T> onPermitted(Runnable callback) {
                this.onPermitted = checkNotNull(callback, "Permitted callback must be set");
                return this;
            }

            @Override
            public RateLimitBuilder<T> onRejected(Runnable callback) {
                this.onRejected = checkNotNull(callback, "Rejected callback must be set");
                return this;
            }

            @Override
            public Builder<T> done() {
                parent.rateLimitBuilder = this;
                return parent;
            }

            @Override
            public io.smallrye.faulttolerance.api.RateLimit get() {
                return new io.smallrye.faulttolerance.api.RateLimit() {
                    @Override
                    public int value() {
                        return maxInvocations;
                    }

                    @Override
                    public long window() {
                        return timeWindow;
                    }

                    @Override
                    public ChronoUnit windowUnit() {
                        return timeWindowUnit;
                    }

                    @Override
                    public long minSpacing() {
                        return minSpacing;
                    }

                    @Override
                    public ChronoUnit minSpacingUnit() {
                        return minSpacingUnit;
                    }

                    @Override
                    public RateLimitType type() {
                        return type;
                    }

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return io.smallrye.faulttolerance.api.RateLimit.class;
                    }
                };
            }
        }

        static class RetryBuilderImpl<V, T> implements RetryBuilder<T>,
                Supplier<org.eclipse.microprofile.faulttolerance.Retry> {
            private final BuilderImpl<V, T> parent;

            private int maxRetries = 3;
            private long delay = 0;
            private ChronoUnit delayUnit = ChronoUnit.MILLIS;
            private long maxDuration = 180_000;
            private ChronoUnit maxDurationUnit = ChronoUnit.MILLIS;
            private long jitter = 200;
            private ChronoUnit jitterUnit = ChronoUnit.MILLIS;
            private Class<? extends Throwable>[] retryOn = new Class[] { Exception.class };
            private Class<? extends Throwable>[] abortOn = new Class[] {};
            private boolean setBasedExceptionDecisionDefined = false;
            private Predicate<Throwable> whenExceptionPredicate;
            private Predicate<Object> whenResultPredicate;
            private Consumer<Throwable> beforeRetry;

            private ExponentialBackoffBuilderImpl<V, T> exponentialBackoffBuilder;
            private FibonacciBackoffBuilderImpl<V, T> fibonacciBackoffBuilder;
            private CustomBackoffBuilderImpl<V, T> customBackoffBuilder;

            private Runnable onRetry;
            private Runnable onSuccess;
            private Runnable onFailure;

            RetryBuilderImpl(BuilderImpl<V, T> parent) {
                this.parent = parent;
            }

            @Override
            public RetryBuilder<T> maxRetries(int value) {
                this.maxRetries = check(value, value >= -1, "Max retries must be >= -1");
                return this;
            }

            @Override
            public RetryBuilder<T> delay(long value, ChronoUnit unit) {
                check(value, value >= 0, "Delay must be >= 0");
                checkNotNull(unit, "Delay unit must be set");

                this.delay = value;
                this.delayUnit = unit;
                return this;
            }

            @Override
            public RetryBuilder<T> maxDuration(long value, ChronoUnit unit) {
                check(value, value >= 0, "Max duration must be >= 0");
                checkNotNull(unit, "Max duration unit must be set");

                this.maxDuration = value;
                this.maxDurationUnit = unit;
                return this;
            }

            @Override
            public RetryBuilder<T> jitter(long value, ChronoUnit unit) {
                check(value, value >= 0, "Jitter must be >= 0");
                checkNotNull(unit, "Jitter unit must be set");

                this.jitter = value;
                this.jitterUnit = unit;
                return this;
            }

            @Override
            public RetryBuilder<T> retryOn(Collection<Class<? extends Throwable>> value) {
                this.retryOn = checkNotNull(value, "Exceptions to retry on must be set").toArray(new Class[0]);
                this.setBasedExceptionDecisionDefined = true;
                return this;
            }

            @Override
            public RetryBuilder<T> retryOn(Class<? extends Throwable> value) {
                this.retryOn = new Class[] { checkNotNull(value, "Exception to retry on must be set") };
                this.setBasedExceptionDecisionDefined = true;
                return this;
            }

            @Override
            public RetryBuilder<T> abortOn(Collection<Class<? extends Throwable>> value) {
                this.abortOn = checkNotNull(value, "Exceptions to abort retrying on must be set").toArray(new Class[0]);
                this.setBasedExceptionDecisionDefined = true;
                return this;
            }

            @Override
            public RetryBuilder<T> abortOn(Class<? extends Throwable> value) {
                this.abortOn = new Class[] { checkNotNull(value, "Exception to abort retrying on must be set") };
                this.setBasedExceptionDecisionDefined = true;
                return this;
            }

            @Override
            public RetryBuilder<T> whenResult(Predicate<Object> value) {
                this.whenResultPredicate = checkNotNull(value, "Result predicate must be set");
                return this;
            }

            @Override
            public RetryBuilder<T> whenException(Predicate<Throwable> value) {
                this.whenExceptionPredicate = checkNotNull(value, "Exception predicate must be set");
                return this;
            }

            @Override
            public RetryBuilder<T> beforeRetry(Runnable value) {
                checkNotNull(value, "Before retry handler must be set");
                this.beforeRetry = ignored -> value.run();
                return this;
            }

            @Override
            public RetryBuilder<T> beforeRetry(Consumer<Throwable> value) {
                this.beforeRetry = checkNotNull(value, "Before retry handler must be set");
                return this;
            }

            @Override
            public ExponentialBackoffBuilder<T> withExponentialBackoff() {
                return new ExponentialBackoffBuilderImpl<>(this);
            }

            @Override
            public FibonacciBackoffBuilder<T> withFibonacciBackoff() {
                return new FibonacciBackoffBuilderImpl<>(this);
            }

            @Override
            public CustomBackoffBuilder<T> withCustomBackoff() {
                return new CustomBackoffBuilderImpl<>(this);
            }

            @Override
            public RetryBuilder<T> onRetry(Runnable callback) {
                this.onRetry = checkNotNull(callback, "Retry callback must be set");
                return this;
            }

            @Override
            public RetryBuilder<T> onSuccess(Runnable callback) {
                this.onSuccess = checkNotNull(callback, "Success callback must be set");
                return this;
            }

            @Override
            public RetryBuilder<T> onFailure(Runnable callback) {
                this.onFailure = checkNotNull(callback, "Failure callback must be set");
                return this;
            }

            @Override
            public Builder<T> done() {
                if (whenExceptionPredicate != null && setBasedExceptionDecisionDefined) {
                    throw new IllegalStateException("The whenException() method may not be combined with retryOn()/abortOn()");
                }

                int backoffStrategies = 0;
                if (exponentialBackoffBuilder != null) {
                    backoffStrategies++;
                }
                if (fibonacciBackoffBuilder != null) {
                    backoffStrategies++;
                }
                if (customBackoffBuilder != null) {
                    backoffStrategies++;
                }
                if (backoffStrategies > 1) {
                    throw new IllegalStateException("Only one backoff strategy may be set for retry");
                }

                parent.retryBuilder = this;
                return parent;
            }

            @Override
            public org.eclipse.microprofile.faulttolerance.Retry get() {
                return new org.eclipse.microprofile.faulttolerance.Retry() {
                    @Override
                    public int maxRetries() {
                        return maxRetries;
                    }

                    @Override
                    public long delay() {
                        return delay;
                    }

                    @Override
                    public ChronoUnit delayUnit() {
                        return delayUnit;
                    }

                    @Override
                    public long maxDuration() {
                        return maxDuration;
                    }

                    @Override
                    public ChronoUnit durationUnit() {
                        return maxDurationUnit;
                    }

                    @Override
                    public long jitter() {
                        return jitter;
                    }

                    @Override
                    public ChronoUnit jitterDelayUnit() {
                        return jitterUnit;
                    }

                    @Override
                    public Class<? extends Throwable>[] retryOn() {
                        return retryOn;
                    }

                    @Override
                    public Class<? extends Throwable>[] abortOn() {
                        return abortOn;
                    }

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return org.eclipse.microprofile.faulttolerance.Retry.class;
                    }
                };
            }

            static class ExponentialBackoffBuilderImpl<V, T> implements ExponentialBackoffBuilder<T>,
                    Supplier<ExponentialBackoff> {
                private final RetryBuilderImpl<V, T> parent;

                private int factor = 2;
                private long maxDelay = 60_000;
                private ChronoUnit maxDelayUnit = ChronoUnit.MILLIS;

                ExponentialBackoffBuilderImpl(RetryBuilderImpl<V, T> parent) {
                    this.parent = parent;
                }

                @Override
                public ExponentialBackoffBuilder<T> factor(int value) {
                    this.factor = check(value, value >= 1, "Factor must be >= 1");
                    return this;
                }

                @Override
                public ExponentialBackoffBuilder<T> maxDelay(long value, ChronoUnit unit) {
                    check(value, value >= 0, "Max delay must be >= 0");
                    checkNotNull(unit, "Max delay unit must be set");

                    this.maxDelay = value;
                    this.maxDelayUnit = unit;
                    return this;
                }

                @Override
                public RetryBuilder<T> done() {
                    parent.exponentialBackoffBuilder = this;
                    return parent;
                }

                @Override
                public ExponentialBackoff get() {
                    return new ExponentialBackoff() {
                        @Override
                        public int factor() {
                            return factor;
                        }

                        @Override
                        public long maxDelay() {
                            return maxDelay;
                        }

                        @Override
                        public ChronoUnit maxDelayUnit() {
                            return maxDelayUnit;
                        }

                        @Override
                        public Class<? extends Annotation> annotationType() {
                            return ExponentialBackoff.class;
                        }
                    };
                }
            }

            static class FibonacciBackoffBuilderImpl<V, T> implements FibonacciBackoffBuilder<T>,
                    Supplier<FibonacciBackoff> {
                private final RetryBuilderImpl<V, T> parent;

                private long maxDelay = 60_000;
                private ChronoUnit maxDelayUnit = ChronoUnit.MILLIS;

                FibonacciBackoffBuilderImpl(RetryBuilderImpl<V, T> parent) {
                    this.parent = parent;
                }

                @Override
                public FibonacciBackoffBuilder<T> maxDelay(long value, ChronoUnit unit) {
                    check(value, value >= 0, "Max delay must be >= 0");
                    checkNotNull(unit, "Max delay unit must be set");

                    this.maxDelay = value;
                    this.maxDelayUnit = unit;
                    return this;
                }

                @Override
                public RetryBuilder<T> done() {
                    parent.fibonacciBackoffBuilder = this;
                    return parent;
                }

                @Override
                public FibonacciBackoff get() {
                    return new FibonacciBackoff() {
                        @Override
                        public long maxDelay() {
                            return maxDelay;
                        }

                        @Override
                        public ChronoUnit maxDelayUnit() {
                            return maxDelayUnit;
                        }

                        @Override
                        public Class<? extends Annotation> annotationType() {
                            return FibonacciBackoff.class;
                        }
                    };
                }
            }

            static class CustomBackoffBuilderImpl<V, T> implements CustomBackoffBuilder<T> {
                private final RetryBuilderImpl<V, T> parent;

                private Supplier<CustomBackoffStrategy> strategy;

                CustomBackoffBuilderImpl(RetryBuilderImpl<V, T> parent) {
                    this.parent = parent;
                }

                @Override
                public CustomBackoffBuilder<T> strategy(Supplier<CustomBackoffStrategy> value) {
                    this.strategy = checkNotNull(value, "Custom backoff strategy must be set");
                    return this;
                }

                @Override
                public RetryBuilder<T> done() {
                    checkNotNull(strategy, "Custom backoff strategy must be set");

                    parent.customBackoffBuilder = this;
                    return parent;
                }
            }
        }

        static class TimeoutBuilderImpl<V, T> implements TimeoutBuilder<T>,
                Supplier<org.eclipse.microprofile.faulttolerance.Timeout> {
            private final BuilderImpl<V, T> parent;

            private long value = 1000;
            private ChronoUnit unit = ChronoUnit.MILLIS;

            private Runnable onTimeout;
            private Runnable onFinished;

            TimeoutBuilderImpl(BuilderImpl<V, T> parent) {
                this.parent = parent;
            }

            @Override
            public TimeoutBuilder<T> duration(long value, ChronoUnit unit) {
                check(value, value >= 0, "Timeout duration must be >= 0");
                checkNotNull(unit, "Timeout duration unit must be set");

                this.value = value;
                this.unit = unit;
                return this;
            }

            @Override
            public TimeoutBuilder<T> onTimeout(Runnable callback) {
                this.onTimeout = checkNotNull(callback, "Timeout callback must be set");
                return this;
            }

            @Override
            public TimeoutBuilder<T> onFinished(Runnable callback) {
                this.onFinished = checkNotNull(callback, "Finished callback must be set");
                return this;
            }

            @Override
            public Builder<T> done() {
                parent.timeoutBuilder = this;
                return parent;
            }

            @Override
            public org.eclipse.microprofile.faulttolerance.Timeout get() {
                return new org.eclipse.microprofile.faulttolerance.Timeout() {
                    @Override
                    public long value() {
                        return value;
                    }

                    @Override
                    public ChronoUnit unit() {
                        return unit;
                    }

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return org.eclipse.microprofile.faulttolerance.Timeout.class;
                    }
                };
            }
        }
    }
}
