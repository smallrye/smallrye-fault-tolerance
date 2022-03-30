package io.smallrye.faulttolerance.core.apiimpl;

import static io.smallrye.faulttolerance.core.Invocation.invocation;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.faulttolerance.api.CustomBackoffStrategy;
import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.async.CompletionStageExecution;
import io.smallrye.faulttolerance.core.async.RememberEventLoop;
import io.smallrye.faulttolerance.core.bulkhead.CompletionStageThreadPoolBulkhead;
import io.smallrye.faulttolerance.core.bulkhead.SemaphoreBulkhead;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreaker;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerEvents;
import io.smallrye.faulttolerance.core.circuit.breaker.CompletionStageCircuitBreaker;
import io.smallrye.faulttolerance.core.fallback.CompletionStageFallback;
import io.smallrye.faulttolerance.core.fallback.Fallback;
import io.smallrye.faulttolerance.core.fallback.FallbackFunction;
import io.smallrye.faulttolerance.core.invocation.AsyncSupport;
import io.smallrye.faulttolerance.core.invocation.AsyncSupportRegistry;
import io.smallrye.faulttolerance.core.invocation.Invoker;
import io.smallrye.faulttolerance.core.invocation.StrategyInvoker;
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
import io.smallrye.faulttolerance.core.timeout.CompletionStageTimeout;
import io.smallrye.faulttolerance.core.timeout.Timeout;
import io.smallrye.faulttolerance.core.timeout.TimerTimeoutWatcher;
import io.smallrye.faulttolerance.core.util.DirectExecutor;
import io.smallrye.faulttolerance.core.util.ExceptionDecision;
import io.smallrye.faulttolerance.core.util.Preconditions;
import io.smallrye.faulttolerance.core.util.PredicateBasedExceptionDecision;
import io.smallrye.faulttolerance.core.util.SetBasedExceptionDecision;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;

// V = value type, e.g. String
// S = strategy type, e.g. String or CompletionStage<String>
// T = result type, e.g. String or CompletionStage<String> or Uni<String>
//
// in synchronous scenario, V = S = T
// in asynchronous scenario, S = CompletionStage<V> and T is an async type that eventually produces V
public final class FaultToleranceImpl<V, S, T> implements FaultTolerance<T> {
    private final FaultToleranceStrategy<S> strategy;
    private final AsyncSupport<V, T> asyncSupport;
    private final EventHandlers eventHandlers;

    // Circuit breakers created using the programmatic API are registered with `CircuitBreakerMaintenance`
    // in two phases:
    //
    // 1. The name is registered eagerly, during `BuilderImpl.build()`, so that `CircuitBreakerMaintenance` methods
    //    may be called immediately after building the `FaultTolerance` instance. A single name may be registered
    //    multiple times.
    // 2. The circuit breaker instance is registered lazily, during `FaultToleranceImpl.call()`, so that
    //    a `FaultTolerance` instance that is created but never used doesn't prevent an actually useful
    //    circuit breaker from being registered later. Only one circuit breaker may be registered
    //    for given name.
    //
    // Lazy registration of circuit breakers exists to allow normal-scoped CDI beans to declare `FaultTolerance`
    // fields that are assigned inline (which effectively means in a constructor). Normal-scoped beans always
    // have a client proxy, whose class inherits from the bean class and calls the superclass's zero-parameter
    // constructor. This leads to the client proxy instance also having an instance of `FaultTolerance`,
    // but that instance is never used. The useful `FaultTolerance` instance is held by the actual bean instance,
    // which is created lazily, on the first method invocation on the client proxy.

    FaultToleranceImpl(FaultToleranceStrategy<S> strategy, AsyncSupport<V, T> asyncSupport, EventHandlers eventHandlers) {
        this.strategy = strategy;
        this.asyncSupport = asyncSupport;
        this.eventHandlers = eventHandlers;
    }

    @Override
    public T call(Callable<T> action) throws Exception {
        if (asyncSupport == null) {
            InvocationContext<T> ctx = new InvocationContext<>(action);
            eventHandlers.register(ctx);
            return ((FaultToleranceStrategy<T>) strategy).apply(ctx);
        }

        Invoker<T> invoker = new CallableInvoker<>(action);
        InvocationContext<CompletionStage<V>> ctx = new InvocationContext<>(() -> asyncSupport.toCompletionStage(invoker));
        eventHandlers.register(ctx);
        Invoker<CompletionStage<V>> wrapper = new StrategyInvoker<>(null,
                (FaultToleranceStrategy<CompletionStage<V>>) strategy, ctx);
        return asyncSupport.fromCompletionStage(wrapper);
    }

    public static final class BuilderImpl<T, R> implements Builder<T, R> {
        private final BuilderEagerDependencies eagerDependencies;
        private final Supplier<BuilderLazyDependencies> lazyDependencies;
        private final boolean isAsync;
        private final Class<?> asyncType; // null when isAsync == false
        private final Function<FaultTolerance<T>, R> finisher;

        private String description;
        private BulkheadBuilderImpl<T, R> bulkheadBuilder;
        private CircuitBreakerBuilderImpl<T, R> circuitBreakerBuilder;
        private FallbackBuilderImpl<T, R> fallbackBuilder;
        private RetryBuilderImpl<T, R> retryBuilder;
        private TimeoutBuilderImpl<T, R> timeoutBuilder;
        private boolean offloadToAnotherThread;

        public BuilderImpl(BuilderEagerDependencies eagerDependencies, Supplier<BuilderLazyDependencies> lazyDependencies,
                Class<?> asyncType, Function<FaultTolerance<T>, R> finisher) {
            this.eagerDependencies = eagerDependencies;
            this.lazyDependencies = lazyDependencies;
            this.isAsync = asyncType != null;
            this.asyncType = asyncType;
            this.finisher = finisher;

            this.description = UUID.randomUUID().toString();
        }

        @Override
        public Builder<T, R> withDescription(String value) {
            this.description = Preconditions.checkNotNull(value, "Description must be set");
            return this;
        }

        @Override
        public BulkheadBuilder<T, R> withBulkhead() {
            return new BulkheadBuilderImpl<>(this);
        }

        @Override
        public CircuitBreakerBuilder<T, R> withCircuitBreaker() {
            return new CircuitBreakerBuilderImpl<>(this);
        }

        @Override
        public FallbackBuilder<T, R> withFallback() {
            return new FallbackBuilderImpl<>(this);
        }

        @Override
        public RetryBuilder<T, R> withRetry() {
            return new RetryBuilderImpl<>(this);
        }

        @Override
        public TimeoutBuilder<T, R> withTimeout() {
            return new TimeoutBuilderImpl<>(this);
        }

        @Override
        public Builder<T, R> withThreadOffload(boolean value) {
            if (!isAsync) {
                throw new IllegalStateException("Thread offload may only be set for asynchronous invocations");
            }

            this.offloadToAnotherThread = value;
            return this;
        }

        @Override
        public R build() {
            eagerInitialization();

            FaultTolerance<T> faultTolerance = new LazyFaultTolerance<>(() -> build(lazyDependencies.get()), asyncType);
            return finisher.apply(faultTolerance);
        }

        // must not access lazyDependencies
        private void eagerInitialization() {
            if (circuitBreakerBuilder != null && circuitBreakerBuilder.name != null) {
                eagerDependencies.cbMaintenance().registerName(circuitBreakerBuilder.name);
            }
        }

        private FaultTolerance<T> build(BuilderLazyDependencies lazyDependencies) {
            Consumer<CircuitBreakerEvents.StateTransition> cbMaintenanceEventHandler = null;
            if (circuitBreakerBuilder != null && circuitBreakerBuilder.name != null) {
                cbMaintenanceEventHandler = eagerDependencies.cbMaintenance()
                        .stateTransitionEventHandler(circuitBreakerBuilder.name);
            }
            EventHandlers eventHandlers = new EventHandlers(
                    bulkheadBuilder != null ? bulkheadBuilder.onAccepted : null,
                    bulkheadBuilder != null ? bulkheadBuilder.onRejected : null,
                    bulkheadBuilder != null ? bulkheadBuilder.onFinished : null,
                    cbMaintenanceEventHandler,
                    circuitBreakerBuilder != null ? circuitBreakerBuilder.onStateChange : null,
                    circuitBreakerBuilder != null ? circuitBreakerBuilder.onSuccess : null,
                    circuitBreakerBuilder != null ? circuitBreakerBuilder.onFailure : null,
                    circuitBreakerBuilder != null ? circuitBreakerBuilder.onPrevented : null,
                    retryBuilder != null ? retryBuilder.onRetry : null,
                    retryBuilder != null ? retryBuilder.onSuccess : null,
                    retryBuilder != null ? retryBuilder.onFailure : null,
                    timeoutBuilder != null ? timeoutBuilder.onTimeout : null,
                    timeoutBuilder != null ? timeoutBuilder.onFinished : null);

            return isAsync ? buildAsync(lazyDependencies, eventHandlers) : buildSync(lazyDependencies, eventHandlers);
        }

        private FaultTolerance<T> buildSync(BuilderLazyDependencies lazyDependencies, EventHandlers eventHandlers) {
            FaultToleranceStrategy<T> strategy = buildSyncStrategy(lazyDependencies);
            return new FaultToleranceImpl<>(strategy, (AsyncSupport<T, T>) null, eventHandlers);
        }

        private <V> FaultTolerance<T> buildAsync(BuilderLazyDependencies lazyDependencies, EventHandlers eventHandlers) {
            FaultToleranceStrategy<CompletionStage<V>> strategy = buildAsyncStrategy(lazyDependencies);
            AsyncSupport<V, T> asyncSupport = AsyncSupportRegistry.get(new Class[0], asyncType);
            return new FaultToleranceImpl<>(strategy, asyncSupport, eventHandlers);
        }

        private FaultToleranceStrategy<T> buildSyncStrategy(BuilderLazyDependencies lazyDependencies) {
            FaultToleranceStrategy<T> result = invocation();

            if (lazyDependencies.ftEnabled() && bulkheadBuilder != null) {
                result = new SemaphoreBulkhead<>(result, description, bulkheadBuilder.limit);
            }

            if (lazyDependencies.ftEnabled() && timeoutBuilder != null) {
                result = new Timeout<>(result, description, timeoutBuilder.durationInMillis,
                        new TimerTimeoutWatcher(lazyDependencies.timer()));
            }

            if (lazyDependencies.ftEnabled() && circuitBreakerBuilder != null) {
                result = new CircuitBreaker<>(result, description,
                        createExceptionDecision(circuitBreakerBuilder.skipOn, circuitBreakerBuilder.failOn,
                                circuitBreakerBuilder.whenPredicate),
                        circuitBreakerBuilder.delayInMillis,
                        circuitBreakerBuilder.requestVolumeThreshold,
                        circuitBreakerBuilder.failureRatio,
                        circuitBreakerBuilder.successThreshold,
                        new SystemStopwatch());

                if (circuitBreakerBuilder.name != null) {
                    CircuitBreaker<?> circuitBreaker = (CircuitBreaker<?>) result;
                    eagerDependencies.cbMaintenance().register(circuitBreakerBuilder.name, circuitBreaker);
                }
            }

            if (lazyDependencies.ftEnabled() && retryBuilder != null) {
                Supplier<BackOff> backoff = prepareRetryBackoff(retryBuilder);

                result = new Retry<>(result, description,
                        createExceptionDecision(retryBuilder.abortOn, retryBuilder.retryOn, retryBuilder.whenPredicate),
                        retryBuilder.maxRetries, retryBuilder.maxDurationInMillis, () -> new ThreadSleepDelay(backoff.get()),
                        new SystemStopwatch());
            }

            // fallback is always enabled
            if (fallbackBuilder != null) {
                FallbackFunction<T> fallbackFunction = ctx -> fallbackBuilder.handler.apply(ctx.failure);
                result = new Fallback<>(result, description, fallbackFunction,
                        createExceptionDecision(fallbackBuilder.skipOn, fallbackBuilder.applyOn,
                                fallbackBuilder.whenPredicate));
            }

            return result;
        }

        private <V> FaultToleranceStrategy<CompletionStage<V>> buildAsyncStrategy(BuilderLazyDependencies lazyDependencies) {
            FaultToleranceStrategy<CompletionStage<V>> result = invocation();

            // thread offload is always enabled
            Executor executor = offloadToAnotherThread ? lazyDependencies.asyncExecutor() : DirectExecutor.INSTANCE;
            result = new CompletionStageExecution<>(result, executor);

            if (lazyDependencies.ftEnabled() && bulkheadBuilder != null) {
                result = new CompletionStageThreadPoolBulkhead<>(result, description, bulkheadBuilder.limit,
                        bulkheadBuilder.queueSize);
            }

            if (lazyDependencies.ftEnabled() && timeoutBuilder != null) {
                result = new CompletionStageTimeout<>(result, description, timeoutBuilder.durationInMillis,
                        new TimerTimeoutWatcher(lazyDependencies.timer()));
            }

            if (lazyDependencies.ftEnabled() && circuitBreakerBuilder != null) {
                result = new CompletionStageCircuitBreaker<>(result, description,
                        createExceptionDecision(circuitBreakerBuilder.skipOn, circuitBreakerBuilder.failOn,
                                circuitBreakerBuilder.whenPredicate),
                        circuitBreakerBuilder.delayInMillis,
                        circuitBreakerBuilder.requestVolumeThreshold,
                        circuitBreakerBuilder.failureRatio,
                        circuitBreakerBuilder.successThreshold,
                        new SystemStopwatch());

                if (circuitBreakerBuilder.name != null) {
                    CircuitBreaker<?> circuitBreaker = (CircuitBreaker<?>) result;
                    eagerDependencies.cbMaintenance().register(circuitBreakerBuilder.name, circuitBreaker);
                }
            }

            if (lazyDependencies.ftEnabled() && retryBuilder != null) {
                Supplier<BackOff> backoff = prepareRetryBackoff(retryBuilder);

                result = new CompletionStageRetry<>(result, description,
                        createExceptionDecision(retryBuilder.abortOn, retryBuilder.retryOn, retryBuilder.whenPredicate),
                        retryBuilder.maxRetries, retryBuilder.maxDurationInMillis,
                        () -> new TimerDelay(backoff.get(), lazyDependencies.timer()),
                        new SystemStopwatch());
            }

            // fallback is always enabled
            if (fallbackBuilder != null) {
                AsyncSupport<V, T> asyncSupport = AsyncSupportRegistry.get(new Class[0], asyncType);
                if (asyncSupport == null) {
                    throw new FaultToleranceException("Unknown async type: " + asyncType);
                }

                FallbackFunction<CompletionStage<V>> fallbackFunction = ctx -> asyncSupport.fallbackResultToCompletionStage(
                        fallbackBuilder.handler.apply(ctx.failure));

                result = new CompletionStageFallback<>(result, description, fallbackFunction,
                        createExceptionDecision(fallbackBuilder.skipOn, fallbackBuilder.applyOn,
                                fallbackBuilder.whenPredicate));
            }

            // thread offload is always enabled
            if (!offloadToAnotherThread) {
                result = new RememberEventLoop<>(result, lazyDependencies.eventLoop());
            }

            return result;
        }

        private static long getTimeInMs(long time, ChronoUnit unit) {
            return Duration.of(time, unit).toMillis();
        }

        private static ExceptionDecision createExceptionDecision(Collection<Class<? extends Throwable>> consideredExpected,
                Collection<Class<? extends Throwable>> consideredFailure, Predicate<Throwable> whenPredicate) {
            if (whenPredicate != null) {
                // the builder API accepts a predicate that returns `true` when an exception is considered failure,
                // but `PredicateBasedExceptionDecision` accepts a predicate that returns `true` when an exception
                // is considered success -- hence the negation
                return new PredicateBasedExceptionDecision(whenPredicate.negate());
            }
            return new SetBasedExceptionDecision(createSetOfThrowables(consideredFailure),
                    createSetOfThrowables(consideredExpected), true);
        }

        private static SetOfThrowables createSetOfThrowables(Collection<Class<? extends Throwable>> throwableClasses) {
            return throwableClasses == null ? SetOfThrowables.EMPTY : SetOfThrowables.create(throwableClasses);
        }

        private static Supplier<BackOff> prepareRetryBackoff(RetryBuilderImpl<?, ?> retryBuilder) {
            long jitterMs = retryBuilder.jitterInMillis;
            Jitter jitter = jitterMs == 0 ? Jitter.ZERO : new RandomJitter(jitterMs);

            if (retryBuilder.exponentialBackoffBuilder != null) {
                int factor = retryBuilder.exponentialBackoffBuilder.factor;
                long maxDelay = retryBuilder.exponentialBackoffBuilder.maxDelayInMillis;
                return () -> new ExponentialBackOff(retryBuilder.delayInMillis, factor, jitter, maxDelay);
            } else if (retryBuilder.fibonacciBackoffBuilder != null) {
                long maxDelay = retryBuilder.fibonacciBackoffBuilder.maxDelayInMillis;
                return () -> new FibonacciBackOff(retryBuilder.delayInMillis, jitter, maxDelay);
            } else if (retryBuilder.customBackoffBuilder != null) {
                Supplier<CustomBackoffStrategy> strategy = retryBuilder.customBackoffBuilder.strategy;
                return () -> {
                    CustomBackoffStrategy instance = strategy.get();
                    instance.init(retryBuilder.delayInMillis);
                    return new CustomBackOff(instance::nextDelayInMillis);
                };
            } else {
                return () -> new ConstantBackOff(retryBuilder.delayInMillis, jitter);
            }
        }

        static class BulkheadBuilderImpl<T, R> implements BulkheadBuilder<T, R> {
            private final BuilderImpl<T, R> parent;

            private int limit = 10;
            private int queueSize = 10;

            private Runnable onAccepted;
            private Runnable onRejected;
            private Runnable onFinished;

            BulkheadBuilderImpl(BuilderImpl<T, R> parent) {
                this.parent = parent;
            }

            @Override
            public BulkheadBuilder<T, R> limit(int value) {
                this.limit = Preconditions.check(value, value >= 1, "Limit must be >= 1");
                return this;
            }

            @Override
            public BulkheadBuilder<T, R> queueSize(int value) {
                if (!parent.isAsync) {
                    throw new IllegalStateException("Bulkhead queue size may only be set for asynchronous invocations");
                }

                this.queueSize = Preconditions.check(value, value >= 1, "Queue size must be >= 1");
                return this;
            }

            @Override
            public BulkheadBuilder<T, R> onAccepted(Runnable callback) {
                this.onAccepted = Preconditions.checkNotNull(callback, "Accepted callback must be set");
                return this;
            }

            @Override
            public BulkheadBuilder<T, R> onRejected(Runnable callback) {
                this.onRejected = Preconditions.checkNotNull(callback, "Rejected callback must be set");
                return this;
            }

            @Override
            public BulkheadBuilder<T, R> onFinished(Runnable callback) {
                this.onFinished = Preconditions.checkNotNull(callback, "Finished callback must be set");
                return this;
            }

            @Override
            public Builder<T, R> done() {
                parent.bulkheadBuilder = this;
                return parent;
            }
        }

        static class CircuitBreakerBuilderImpl<T, R> implements CircuitBreakerBuilder<T, R> {
            private final BuilderImpl<T, R> parent;

            private Collection<Class<? extends Throwable>> failOn = Collections.singleton(Throwable.class);
            private Collection<Class<? extends Throwable>> skipOn = Collections.emptySet();
            private boolean setBasedExceptionDecisionDefined = false;
            private Predicate<Throwable> whenPredicate;
            private long delayInMillis = 5000;
            private int requestVolumeThreshold = 20;
            private double failureRatio = 0.5;
            private int successThreshold = 1;

            private String name; // unnamed by default

            private Consumer<CircuitBreakerState> onStateChange;
            private Runnable onSuccess;
            private Runnable onFailure;
            private Runnable onPrevented;

            CircuitBreakerBuilderImpl(BuilderImpl<T, R> parent) {
                this.parent = parent;
            }

            @Override
            public CircuitBreakerBuilder<T, R> failOn(Collection<Class<? extends Throwable>> value) {
                this.failOn = Preconditions.checkNotNull(value, "Exceptions considered failure must be set");
                this.setBasedExceptionDecisionDefined = true;
                return this;
            }

            @Override
            public CircuitBreakerBuilder<T, R> skipOn(Collection<Class<? extends Throwable>> value) {
                this.skipOn = Preconditions.checkNotNull(value, "Exceptions considered success must be set");
                this.setBasedExceptionDecisionDefined = true;
                return this;
            }

            @Override
            public CircuitBreakerBuilder<T, R> when(Predicate<Throwable> value) {
                this.whenPredicate = Preconditions.checkNotNull(value, "Exception predicate must be set");
                return this;
            }

            @Override
            public CircuitBreakerBuilder<T, R> delay(long value, ChronoUnit unit) {
                Preconditions.check(value, value >= 0, "Delay must be >= 0");
                Preconditions.checkNotNull(unit, "Delay unit must be set");

                this.delayInMillis = getTimeInMs(value, unit);
                return this;
            }

            @Override
            public CircuitBreakerBuilder<T, R> requestVolumeThreshold(int value) {
                this.requestVolumeThreshold = Preconditions.check(value, value >= 1, "Request volume threshold must be >= 1");
                return this;
            }

            @Override
            public CircuitBreakerBuilder<T, R> failureRatio(double value) {
                this.failureRatio = Preconditions.check(value, value >= 0 && value <= 1, "Failure ratio must be >= 0 and <= 1");
                return this;
            }

            @Override
            public CircuitBreakerBuilder<T, R> successThreshold(int value) {
                this.successThreshold = Preconditions.check(value, value >= 1, "Success threshold must be >= 1");
                return this;
            }

            @Override
            public CircuitBreakerBuilder<T, R> name(String value) {
                this.name = Preconditions.checkNotNull(value, "Circuit breaker name must be set");
                return this;
            }

            @Override
            public CircuitBreakerBuilder<T, R> onStateChange(Consumer<CircuitBreakerState> callback) {
                this.onStateChange = Preconditions.checkNotNull(callback, "On state change callback must be set");
                return this;
            }

            @Override
            public CircuitBreakerBuilder<T, R> onSuccess(Runnable callback) {
                this.onSuccess = Preconditions.checkNotNull(callback, "On success callback must be set");
                return this;
            }

            @Override
            public CircuitBreakerBuilder<T, R> onFailure(Runnable callback) {
                this.onFailure = Preconditions.checkNotNull(callback, "On failure callback must be set");
                return this;
            }

            @Override
            public CircuitBreakerBuilder<T, R> onPrevented(Runnable callback) {
                this.onPrevented = Preconditions.checkNotNull(callback, "On prevented callback must be set");
                return this;
            }

            @Override
            public Builder<T, R> done() {
                if (whenPredicate != null && setBasedExceptionDecisionDefined) {
                    throw new IllegalStateException("The when() method may not be combined with failOn() / skipOn()");
                }

                parent.circuitBreakerBuilder = this;
                return parent;
            }
        }

        static class FallbackBuilderImpl<T, R> implements FallbackBuilder<T, R> {
            private final BuilderImpl<T, R> parent;

            private Function<Throwable, T> handler;
            private Collection<Class<? extends Throwable>> applyOn = Collections.singleton(Throwable.class);
            private Collection<Class<? extends Throwable>> skipOn = Collections.emptySet();
            private boolean setBasedExceptionDecisionDefined = false;
            private Predicate<Throwable> whenPredicate;

            FallbackBuilderImpl(BuilderImpl<T, R> parent) {
                this.parent = parent;
            }

            @Override
            public FallbackBuilder<T, R> handler(Supplier<T> value) {
                Preconditions.checkNotNull(value, "Fallback handler must be set");
                this.handler = ignored -> value.get();
                return this;
            }

            @Override
            public FallbackBuilder<T, R> handler(Function<Throwable, T> value) {
                this.handler = Preconditions.checkNotNull(value, "Fallback handler must be set");
                return this;
            }

            @Override
            public FallbackBuilder<T, R> applyOn(Collection<Class<? extends Throwable>> value) {
                this.applyOn = Preconditions.checkNotNull(value, "Exceptions to apply fallback on must be set");
                this.setBasedExceptionDecisionDefined = true;
                return this;
            }

            @Override
            public FallbackBuilder<T, R> skipOn(Collection<Class<? extends Throwable>> value) {
                this.skipOn = Preconditions.checkNotNull(value, "Exceptions to skip fallback on must be set");
                this.setBasedExceptionDecisionDefined = true;
                return this;
            }

            @Override
            public FallbackBuilder<T, R> when(Predicate<Throwable> value) {
                this.whenPredicate = Preconditions.checkNotNull(value, "Exception predicate must be set");
                return this;
            }

            @Override
            public Builder<T, R> done() {
                Preconditions.checkNotNull(handler, "Fallback handler must be set");

                if (whenPredicate != null && setBasedExceptionDecisionDefined) {
                    throw new IllegalStateException("The when() method may not be combined with applyOn() / skipOn()");
                }

                parent.fallbackBuilder = this;
                return parent;
            }
        }

        static class RetryBuilderImpl<T, R> implements RetryBuilder<T, R> {
            private final BuilderImpl<T, R> parent;

            private int maxRetries = 3;
            private long delayInMillis = 0;
            private long maxDurationInMillis = 180_000;
            private long jitterInMillis = 200;
            private Collection<Class<? extends Throwable>> retryOn = Collections.singleton(Exception.class);
            private Collection<Class<? extends Throwable>> abortOn = Collections.emptySet();
            private boolean setBasedExceptionDecisionDefined = false;
            private Predicate<Throwable> whenPredicate;

            private ExponentialBackoffBuilderImpl<T, R> exponentialBackoffBuilder;
            private FibonacciBackoffBuilderImpl<T, R> fibonacciBackoffBuilder;
            private CustomBackoffBuilderImpl<T, R> customBackoffBuilder;

            private Runnable onRetry;
            private Runnable onSuccess;
            private Runnable onFailure;

            RetryBuilderImpl(BuilderImpl<T, R> parent) {
                this.parent = parent;
            }

            @Override
            public RetryBuilder<T, R> maxRetries(int value) {
                this.maxRetries = Preconditions.check(value, value >= -1, "Max retries must be >= -1");
                return this;
            }

            @Override
            public RetryBuilder<T, R> delay(long value, ChronoUnit unit) {
                Preconditions.check(value, value >= 0, "Delay must be >= 0");
                Preconditions.checkNotNull(unit, "Delay unit must be set");

                this.delayInMillis = getTimeInMs(value, unit);
                return this;
            }

            @Override
            public RetryBuilder<T, R> maxDuration(long value, ChronoUnit unit) {
                Preconditions.check(value, value >= 0, "Max duration must be >= 0");
                Preconditions.checkNotNull(unit, "Max duration unit must be set");

                this.maxDurationInMillis = getTimeInMs(value, unit);
                return this;
            }

            @Override
            public RetryBuilder<T, R> jitter(long value, ChronoUnit unit) {
                Preconditions.check(value, value >= 0, "Jitter must be >= 0");
                Preconditions.checkNotNull(unit, "Jitter unit must be set");

                this.jitterInMillis = getTimeInMs(value, unit);
                return this;
            }

            @Override
            public RetryBuilder<T, R> retryOn(Collection<Class<? extends Throwable>> value) {
                this.retryOn = Preconditions.checkNotNull(value, "Exceptions to retry on must be set");
                this.setBasedExceptionDecisionDefined = true;
                return this;
            }

            @Override
            public RetryBuilder<T, R> abortOn(Collection<Class<? extends Throwable>> value) {
                this.abortOn = Preconditions.checkNotNull(value, "Exceptions to abort retrying on must be set");
                this.setBasedExceptionDecisionDefined = true;
                return this;
            }

            @Override
            public RetryBuilder<T, R> when(Predicate<Throwable> value) {
                this.whenPredicate = Preconditions.checkNotNull(value, "Exception predicate must be set");
                return this;
            }

            @Override
            public ExponentialBackoffBuilder<T, R> withExponentialBackoff() {
                return new ExponentialBackoffBuilderImpl<>(this);
            }

            @Override
            public FibonacciBackoffBuilder<T, R> withFibonacciBackoff() {
                return new FibonacciBackoffBuilderImpl<>(this);
            }

            @Override
            public CustomBackoffBuilder<T, R> withCustomBackoff() {
                return new CustomBackoffBuilderImpl<>(this);
            }

            @Override
            public RetryBuilder<T, R> onRetry(Runnable callback) {
                this.onRetry = Preconditions.checkNotNull(callback, "Retry callback must be set");
                return this;
            }

            @Override
            public RetryBuilder<T, R> onSuccess(Runnable callback) {
                this.onSuccess = Preconditions.checkNotNull(callback, "Success callback must be set");
                return this;
            }

            @Override
            public RetryBuilder<T, R> onFailure(Runnable callback) {
                this.onFailure = Preconditions.checkNotNull(callback, "Failure callback must be set");
                return this;
            }

            @Override
            public Builder<T, R> done() {
                if (whenPredicate != null && setBasedExceptionDecisionDefined) {
                    throw new IllegalStateException("The when() method may not be combined with retryOn() / abortOn()");
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

            static class ExponentialBackoffBuilderImpl<T, R> implements ExponentialBackoffBuilder<T, R> {
                private final RetryBuilderImpl<T, R> parent;

                private int factor = 2;
                private long maxDelayInMillis = 60_000;

                ExponentialBackoffBuilderImpl(RetryBuilderImpl<T, R> parent) {
                    this.parent = parent;
                }

                @Override
                public ExponentialBackoffBuilder<T, R> factor(int value) {
                    this.factor = Preconditions.check(value, value >= 1, "Factor must be >= 1");
                    return this;
                }

                @Override
                public ExponentialBackoffBuilder<T, R> maxDelay(long value, ChronoUnit unit) {
                    Preconditions.check(value, value >= 0, "Max delay must be >= 0");
                    Preconditions.checkNotNull(unit, "Max delay unit must be set");

                    this.maxDelayInMillis = getTimeInMs(value, unit);
                    return this;
                }

                @Override
                public RetryBuilder<T, R> done() {
                    parent.exponentialBackoffBuilder = this;
                    return parent;
                }
            }

            static class FibonacciBackoffBuilderImpl<T, R> implements FibonacciBackoffBuilder<T, R> {
                private final RetryBuilderImpl<T, R> parent;

                private long maxDelayInMillis = 60_000;

                FibonacciBackoffBuilderImpl(RetryBuilderImpl<T, R> parent) {
                    this.parent = parent;
                }

                @Override
                public FibonacciBackoffBuilder<T, R> maxDelay(long value, ChronoUnit unit) {
                    Preconditions.check(value, value >= 0, "Max delay must be >= 0");
                    Preconditions.checkNotNull(unit, "Max delay unit must be set");

                    this.maxDelayInMillis = getTimeInMs(value, unit);
                    return this;
                }

                @Override
                public RetryBuilder<T, R> done() {
                    parent.fibonacciBackoffBuilder = this;
                    return parent;
                }
            }

            static class CustomBackoffBuilderImpl<T, R> implements CustomBackoffBuilder<T, R> {
                private final RetryBuilderImpl<T, R> parent;

                private Supplier<CustomBackoffStrategy> strategy;

                CustomBackoffBuilderImpl(RetryBuilderImpl<T, R> parent) {
                    this.parent = parent;
                }

                @Override
                public CustomBackoffBuilder<T, R> strategy(Supplier<CustomBackoffStrategy> value) {
                    this.strategy = Preconditions.checkNotNull(value, "Custom backoff strategy must be set");
                    return this;
                }

                @Override
                public RetryBuilder<T, R> done() {
                    Preconditions.checkNotNull(strategy, "Custom backoff strategy must be set");

                    parent.customBackoffBuilder = this;
                    return parent;
                }
            }
        }

        static class TimeoutBuilderImpl<T, R> implements TimeoutBuilder<T, R> {
            private final BuilderImpl<T, R> parent;

            private long durationInMillis = 1000;

            private Runnable onTimeout;
            private Runnable onFinished;

            TimeoutBuilderImpl(BuilderImpl<T, R> parent) {
                this.parent = parent;
            }

            @Override
            public TimeoutBuilder<T, R> duration(long value, ChronoUnit unit) {
                Preconditions.check(value, value >= 0, "Timeout duration must be >= 0");
                Preconditions.checkNotNull(unit, "Timeout duration unit must be set");

                this.durationInMillis = getTimeInMs(value, unit);
                return this;
            }

            @Override
            public TimeoutBuilder<T, R> onTimeout(Runnable callback) {
                this.onTimeout = Preconditions.checkNotNull(callback, "Timeout callback must be set");
                return this;
            }

            @Override
            public TimeoutBuilder<T, R> onFinished(Runnable callback) {
                this.onFinished = Preconditions.checkNotNull(callback, "Finished callback must be set");
                return this;
            }

            @Override
            public Builder<T, R> done() {
                parent.timeoutBuilder = this;
                return parent;
            }
        }
    }
}
