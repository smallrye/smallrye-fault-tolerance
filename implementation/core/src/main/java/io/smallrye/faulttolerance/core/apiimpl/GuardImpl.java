package io.smallrye.faulttolerance.core.apiimpl;

import static io.smallrye.faulttolerance.core.Invocation.invocation;
import static io.smallrye.faulttolerance.core.util.Durations.timeInMillis;
import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;

import java.lang.reflect.Type;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.enterprise.util.TypeLiteral;

import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.faulttolerance.api.CustomBackoffStrategy;
import io.smallrye.faulttolerance.api.Guard;
import io.smallrye.faulttolerance.api.RateLimitType;
import io.smallrye.faulttolerance.core.FaultToleranceContext;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.async.RememberEventLoop;
import io.smallrye.faulttolerance.core.async.SyncAsyncSplit;
import io.smallrye.faulttolerance.core.async.ThreadOffload;
import io.smallrye.faulttolerance.core.bulkhead.Bulkhead;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreaker;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerEvents;
import io.smallrye.faulttolerance.core.fallback.Fallback;
import io.smallrye.faulttolerance.core.fallback.FallbackFunction;
import io.smallrye.faulttolerance.core.invocation.AsyncSupport;
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
import io.smallrye.faulttolerance.core.util.Preconditions;
import io.smallrye.faulttolerance.core.util.PredicateBasedExceptionDecision;
import io.smallrye.faulttolerance.core.util.PredicateBasedResultDecision;
import io.smallrye.faulttolerance.core.util.ResultDecision;
import io.smallrye.faulttolerance.core.util.SetBasedExceptionDecision;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;

public class GuardImpl implements Guard {
    final FaultToleranceStrategy<?> strategy;
    final EventHandlers eventHandlers;

    // Circuit breakers created using the programmatic API are registered with `CircuitBreakerMaintenance`
    // in two phases:
    //
    // 1. The name is registered eagerly, during `BuilderImpl.build()`, so that `CircuitBreakerMaintenance` methods
    //    may be called immediately after building the `Guard` instance. A single name may be registered
    //    multiple times.
    // 2. The circuit breaker instance is registered lazily, during `BuilderImpl.buildStrategy()`, so that
    //    a `Guard` instance that is created but never used doesn't prevent an actually useful
    //    circuit breaker from being registered later. Only one circuit breaker may be registered
    //    for given name.
    //
    // Lazy registration of circuit breakers exists to allow normal-scoped CDI beans to declare `Guard`
    // fields that are assigned inline (which effectively means in a constructor). Normal-scoped beans always
    // have a client proxy, whose class inherits from the bean class and calls the superclass's zero-parameter
    // constructor. This leads to the client proxy instance also having an instance of `Guard`,
    // but that instance is never used. The useful `Guard` instance is held by the actual bean instance,
    // which is created lazily, on the first method invocation on the client proxy.

    GuardImpl(FaultToleranceStrategy<?> strategy, EventHandlers eventHandlers) {
        this.strategy = strategy;
        this.eventHandlers = eventHandlers;
    }

    public <V, T> T guard(Callable<T> action, Type valueType, Consumer<FaultToleranceContext<?>> contextModifier)
            throws Exception {
        AsyncSupport<V, T> asyncSupport = GuardCommon.asyncSupport(valueType);
        return GuardCommon.guard(action, (FaultToleranceStrategy<V>) strategy, asyncSupport, eventHandlers,
                contextModifier);
    }

    @Override
    public <T> T call(Callable<T> action, Class<T> type) throws Exception {
        return guard(action, type, null);
    }

    @Override
    public <T> T call(Callable<T> action, TypeLiteral<T> type) throws Exception {
        return guard(action, type.getType(), null);
    }

    @Override
    public <T> T get(Supplier<T> action, Class<T> type) {
        try {
            return guard(action::get, type, null);
        } catch (Exception e) {
            throw sneakyThrow(e);
        }
    }

    @Override
    public <T> T get(Supplier<T> action, TypeLiteral<T> type) {
        try {
            return guard(action::get, type.getType(), null);
        } catch (Exception e) {
            throw sneakyThrow(e);
        }
    }

    public static class BuilderImpl implements Builder {
        private final BuilderEagerDependencies eagerDependencies;
        private final Supplier<BuilderLazyDependencies> lazyDependencies;
        private String description;
        private BulkheadBuilderImpl bulkheadBuilder;
        private CircuitBreakerBuilderImpl circuitBreakerBuilder;
        private RateLimitBuilderImpl rateLimitBuilder;
        private RetryBuilderImpl retryBuilder;
        private TimeoutBuilderImpl timeoutBuilder;
        private boolean offloadToAnotherThread;
        private Executor offloadExecutor;

        public BuilderImpl(BuilderEagerDependencies eagerDependencies, Supplier<BuilderLazyDependencies> lazyDependencies) {
            this.eagerDependencies = eagerDependencies;
            this.lazyDependencies = lazyDependencies;

            this.description = UUID.randomUUID().toString();
        }

        @Override
        public Builder withDescription(String value) {
            this.description = Preconditions.checkNotNull(value, "Description must be set");
            return this;
        }

        @Override
        public BulkheadBuilder withBulkhead() {
            return new BulkheadBuilderImpl(this);
        }

        @Override
        public CircuitBreakerBuilder withCircuitBreaker() {
            return new CircuitBreakerBuilderImpl(this);
        }

        @Override
        public RateLimitBuilder withRateLimit() {
            return new RateLimitBuilderImpl(this);
        }

        @Override
        public RetryBuilder withRetry() {
            return new RetryBuilderImpl(this);
        }

        @Override
        public TimeoutBuilder withTimeout() {
            return new TimeoutBuilderImpl(this);
        }

        @Override
        public Builder withThreadOffload(boolean value) {
            this.offloadToAnotherThread = value;
            return this;
        }

        @Override
        public Builder withThreadOffloadExecutor(Executor executor) {
            this.offloadExecutor = Preconditions.checkNotNull(executor, "Thread offload executor must be set");
            return this;
        }

        @Override
        public Guard build() {
            eagerInitialization();
            return new LazyGuard(() -> new GuardImpl(buildStrategy(lazyDependencies.get()), buildEventHandlers()));
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

        final <V> FaultToleranceStrategy<V> buildStrategy(BuilderLazyDependencies lazyDependencies) {
            FaultToleranceStrategy<V> result = invocation();

            // thread offload is always enabled
            Executor executor = offloadExecutor != null ? offloadExecutor : lazyDependencies.asyncExecutor();
            result = new SyncAsyncSplit<>(new ThreadOffload<>(result, executor, offloadToAnotherThread), result);

            if (lazyDependencies.ftEnabled() && bulkheadBuilder != null) {
                result = new Bulkhead<>(result, description,
                        bulkheadBuilder.limit,
                        bulkheadBuilder.queueSize,
                        bulkheadBuilder.syncQueueingEnabled);
            }

            if (lazyDependencies.ftEnabled() && timeoutBuilder != null) {
                result = new Timeout<>(result, description, timeoutBuilder.durationInMillis,
                        lazyDependencies.timer());
            }

            if (lazyDependencies.ftEnabled() && rateLimitBuilder != null) {
                result = new RateLimit<>(result, description,
                        rateLimitBuilder.maxInvocations,
                        rateLimitBuilder.timeWindowInMillis,
                        rateLimitBuilder.minSpacingInMillis,
                        rateLimitBuilder.type,
                        SystemStopwatch.INSTANCE);
            }

            if (lazyDependencies.ftEnabled() && circuitBreakerBuilder != null) {
                result = new CircuitBreaker<>(result, description,
                        createExceptionDecision(circuitBreakerBuilder.skipOn, circuitBreakerBuilder.failOn,
                                circuitBreakerBuilder.whenPredicate),
                        circuitBreakerBuilder.delayInMillis,
                        circuitBreakerBuilder.requestVolumeThreshold,
                        circuitBreakerBuilder.failureRatio,
                        circuitBreakerBuilder.successThreshold,
                        SystemStopwatch.INSTANCE,
                        lazyDependencies.timer());

                if (circuitBreakerBuilder.name != null) {
                    CircuitBreaker<?> circuitBreaker = (CircuitBreaker<?>) result;
                    eagerDependencies.cbMaintenance().register(circuitBreakerBuilder.name, circuitBreaker);
                }
            }

            if (lazyDependencies.ftEnabled() && retryBuilder != null) {
                Supplier<BackOff> backoff = prepareRetryBackoff(retryBuilder);

                result = new Retry<>(result, description,
                        createResultDecision(retryBuilder.whenResultPredicate),
                        createExceptionDecision(retryBuilder.abortOn, retryBuilder.retryOn,
                                retryBuilder.whenExceptionPredicate),
                        retryBuilder.maxRetries, retryBuilder.maxDurationInMillis,
                        () -> new ThreadSleepDelay(backoff.get()),
                        () -> new TimerDelay(backoff.get(), lazyDependencies.timer()),
                        SystemStopwatch.INSTANCE,
                        retryBuilder.beforeRetry != null ? ctx -> retryBuilder.beforeRetry.accept(ctx.failure) : null);
            }

            // fallback is always enabled
            result = new Fallback<>(result, description, FallbackFunction.ignore(), ExceptionDecision.IGNORE);

            MetricsProvider metricsProvider = lazyDependencies.metricsProvider();
            if (metricsProvider.isEnabled()) {
                MeteredOperation defaultOperation = buildMeteredOperation();
                result = new DelegatingMetricsCollector<>(result, metricsProvider, defaultOperation);
            }

            // thread offload is always enabled
            if (!offloadToAnotherThread) {
                result = new SyncAsyncSplit<>(new RememberEventLoop<>(result, lazyDependencies.eventLoop()), result);
            }

            return result;
        }

        private MeteredOperation buildMeteredOperation() {
            return new BasicMeteredOperationImpl(description, true, bulkheadBuilder != null,
                    circuitBreakerBuilder != null, false, rateLimitBuilder != null,
                    retryBuilder != null, timeoutBuilder != null);
        }

        private static ResultDecision createResultDecision(Predicate<Object> whenResultPredicate) {
            if (whenResultPredicate != null) {
                // the builder API accepts a predicate that returns `true` when a result is considered failure,
                // but `[CompletionStage]Retry` accepts a predicate that returns `true` when a result is
                // considered success -- hence the negation
                return new PredicateBasedResultDecision(whenResultPredicate.negate());
            }
            return ResultDecision.ALWAYS_EXPECTED;
        }

        static ExceptionDecision createExceptionDecision(Collection<Class<? extends Throwable>> consideredExpected,
                Collection<Class<? extends Throwable>> consideredFailure, Predicate<Throwable> whenExceptionPredicate) {
            if (whenExceptionPredicate != null) {
                // the builder API accepts a predicate that returns `true` when an exception is considered failure,
                // but `PredicateBasedExceptionDecision` accepts a predicate that returns `true` when an exception
                // is considered success -- hence the negation
                return new PredicateBasedExceptionDecision(whenExceptionPredicate.negate());
            }
            return new SetBasedExceptionDecision(createSetOfThrowables(consideredFailure),
                    createSetOfThrowables(consideredExpected), true);
        }

        private static SetOfThrowables createSetOfThrowables(Collection<Class<? extends Throwable>> throwableClasses) {
            return throwableClasses == null ? SetOfThrowables.EMPTY : SetOfThrowables.create(throwableClasses);
        }

        private static Supplier<BackOff> prepareRetryBackoff(RetryBuilderImpl retryBuilder) {
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

        static class BulkheadBuilderImpl implements BulkheadBuilder {
            private final BuilderImpl parent;

            private int limit = 10;
            private int queueSize = 10;
            private boolean syncQueueingEnabled;

            private Runnable onAccepted;
            private Runnable onRejected;
            private Runnable onFinished;

            BulkheadBuilderImpl(BuilderImpl parent) {
                this.parent = parent;
            }

            @Override
            public BulkheadBuilder limit(int value) {
                this.limit = Preconditions.check(value, value >= 1, "Limit must be >= 1");
                return this;
            }

            @Override
            public BulkheadBuilder queueSize(int value) {
                this.queueSize = Preconditions.check(value, value >= 1, "Queue size must be >= 1");
                return this;
            }

            @Override
            public BulkheadBuilder enableSynchronousQueueing() {
                this.syncQueueingEnabled = true;
                return this;
            }

            @Override
            public BulkheadBuilder onAccepted(Runnable callback) {
                this.onAccepted = Preconditions.checkNotNull(callback, "Accepted callback must be set");
                return this;
            }

            @Override
            public BulkheadBuilder onRejected(Runnable callback) {
                this.onRejected = Preconditions.checkNotNull(callback, "Rejected callback must be set");
                return this;
            }

            @Override
            public BulkheadBuilder onFinished(Runnable callback) {
                this.onFinished = Preconditions.checkNotNull(callback, "Finished callback must be set");
                return this;
            }

            @Override
            public Builder done() {
                parent.bulkheadBuilder = this;
                return parent;
            }
        }

        static class CircuitBreakerBuilderImpl implements CircuitBreakerBuilder {
            private final BuilderImpl parent;

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

            CircuitBreakerBuilderImpl(BuilderImpl parent) {
                this.parent = parent;
            }

            @Override
            public CircuitBreakerBuilder failOn(Collection<Class<? extends Throwable>> value) {
                this.failOn = Preconditions.checkNotNull(value, "Exceptions considered failure must be set");
                this.setBasedExceptionDecisionDefined = true;
                return this;
            }

            @Override
            public CircuitBreakerBuilder skipOn(Collection<Class<? extends Throwable>> value) {
                this.skipOn = Preconditions.checkNotNull(value, "Exceptions considered success must be set");
                this.setBasedExceptionDecisionDefined = true;
                return this;
            }

            @Override
            public CircuitBreakerBuilder when(Predicate<Throwable> value) {
                this.whenPredicate = Preconditions.checkNotNull(value, "Exception predicate must be set");
                return this;
            }

            @Override
            public CircuitBreakerBuilder delay(long value, ChronoUnit unit) {
                Preconditions.check(value, value >= 0, "Delay must be >= 0");
                Preconditions.checkNotNull(unit, "Delay unit must be set");

                this.delayInMillis = timeInMillis(value, unit);
                return this;
            }

            @Override
            public CircuitBreakerBuilder requestVolumeThreshold(int value) {
                this.requestVolumeThreshold = Preconditions.check(value, value >= 1, "Request volume threshold must be >= 1");
                return this;
            }

            @Override
            public CircuitBreakerBuilder failureRatio(double value) {
                this.failureRatio = Preconditions.check(value, value >= 0 && value <= 1, "Failure ratio must be >= 0 and <= 1");
                return this;
            }

            @Override
            public CircuitBreakerBuilder successThreshold(int value) {
                this.successThreshold = Preconditions.check(value, value >= 1, "Success threshold must be >= 1");
                return this;
            }

            @Override
            public CircuitBreakerBuilder name(String value) {
                this.name = Preconditions.checkNotNull(value, "Circuit breaker name must be set");
                return this;
            }

            @Override
            public CircuitBreakerBuilder onStateChange(Consumer<CircuitBreakerState> callback) {
                this.onStateChange = Preconditions.checkNotNull(callback, "On state change callback must be set");
                return this;
            }

            @Override
            public CircuitBreakerBuilder onSuccess(Runnable callback) {
                this.onSuccess = Preconditions.checkNotNull(callback, "On success callback must be set");
                return this;
            }

            @Override
            public CircuitBreakerBuilder onFailure(Runnable callback) {
                this.onFailure = Preconditions.checkNotNull(callback, "On failure callback must be set");
                return this;
            }

            @Override
            public CircuitBreakerBuilder onPrevented(Runnable callback) {
                this.onPrevented = Preconditions.checkNotNull(callback, "On prevented callback must be set");
                return this;
            }

            @Override
            public Builder done() {
                if (whenPredicate != null && setBasedExceptionDecisionDefined) {
                    throw new IllegalStateException("The when() method may not be combined with failOn() / skipOn()");
                }

                parent.circuitBreakerBuilder = this;
                return parent;
            }
        }

        static class RateLimitBuilderImpl implements RateLimitBuilder {
            private final BuilderImpl parent;

            private int maxInvocations = 100;
            private long timeWindowInMillis = 1000;
            private long minSpacingInMillis = 0;
            private RateLimitType type = RateLimitType.FIXED;

            private Runnable onPermitted;
            private Runnable onRejected;

            RateLimitBuilderImpl(BuilderImpl parent) {
                this.parent = parent;
            }

            @Override
            public RateLimitBuilder limit(int value) {
                this.maxInvocations = Preconditions.check(value, value >= 1, "Rate limit must be >= 1");
                return this;
            }

            @Override
            public RateLimitBuilder window(long value, ChronoUnit unit) {
                Preconditions.check(value, value >= 1, "Time window length must be >= 1");
                Preconditions.checkNotNull(unit, "Time window length unit must be set");

                this.timeWindowInMillis = timeInMillis(value, unit);
                return this;
            }

            @Override
            public RateLimitBuilder minSpacing(long value, ChronoUnit unit) {
                Preconditions.check(value, value >= 0, "Min spacing must be >= 0");
                Preconditions.checkNotNull(unit, "Min spacing unit must be set");

                this.minSpacingInMillis = timeInMillis(value, unit);
                return this;
            }

            @Override
            public RateLimitBuilder type(RateLimitType value) {
                this.type = Preconditions.checkNotNull(value, "Time window type must be set");
                return this;
            }

            @Override
            public RateLimitBuilder onPermitted(Runnable callback) {
                this.onPermitted = Preconditions.checkNotNull(callback, "Permitted callback must be set");
                return this;
            }

            @Override
            public RateLimitBuilder onRejected(Runnable callback) {
                this.onRejected = Preconditions.checkNotNull(callback, "Rejected callback must be set");
                return this;
            }

            @Override
            public Builder done() {
                parent.rateLimitBuilder = this;
                return parent;
            }
        }

        static class RetryBuilderImpl implements RetryBuilder {
            private final BuilderImpl parent;

            private int maxRetries = 3;
            private long delayInMillis = 0;
            private long maxDurationInMillis = 180_000;
            private long jitterInMillis = 200;
            private Collection<Class<? extends Throwable>> retryOn = Collections.singleton(Exception.class);
            private Collection<Class<? extends Throwable>> abortOn = Collections.emptySet();
            private boolean setBasedExceptionDecisionDefined = false;
            private Predicate<Throwable> whenExceptionPredicate;
            private Predicate<Object> whenResultPredicate;
            private Consumer<Throwable> beforeRetry;

            private ExponentialBackoffBuilderImpl exponentialBackoffBuilder;
            private FibonacciBackoffBuilderImpl fibonacciBackoffBuilder;
            private CustomBackoffBuilderImpl customBackoffBuilder;

            private Runnable onRetry;
            private Runnable onSuccess;
            private Runnable onFailure;

            RetryBuilderImpl(BuilderImpl parent) {
                this.parent = parent;
            }

            @Override
            public RetryBuilder maxRetries(int value) {
                this.maxRetries = Preconditions.check(value, value >= -1, "Max retries must be >= -1");
                return this;
            }

            @Override
            public RetryBuilder delay(long value, ChronoUnit unit) {
                Preconditions.check(value, value >= 0, "Delay must be >= 0");
                Preconditions.checkNotNull(unit, "Delay unit must be set");

                this.delayInMillis = timeInMillis(value, unit);
                return this;
            }

            @Override
            public RetryBuilder maxDuration(long value, ChronoUnit unit) {
                Preconditions.check(value, value >= 0, "Max duration must be >= 0");
                Preconditions.checkNotNull(unit, "Max duration unit must be set");

                this.maxDurationInMillis = timeInMillis(value, unit);
                return this;
            }

            @Override
            public RetryBuilder jitter(long value, ChronoUnit unit) {
                Preconditions.check(value, value >= 0, "Jitter must be >= 0");
                Preconditions.checkNotNull(unit, "Jitter unit must be set");

                this.jitterInMillis = timeInMillis(value, unit);
                return this;
            }

            @Override
            public RetryBuilder retryOn(Collection<Class<? extends Throwable>> value) {
                this.retryOn = Preconditions.checkNotNull(value, "Exceptions to retry on must be set");
                this.setBasedExceptionDecisionDefined = true;
                return this;
            }

            @Override
            public RetryBuilder abortOn(Collection<Class<? extends Throwable>> value) {
                this.abortOn = Preconditions.checkNotNull(value, "Exceptions to abort retrying on must be set");
                this.setBasedExceptionDecisionDefined = true;
                return this;
            }

            @Override
            public RetryBuilder whenResult(Predicate<Object> value) {
                this.whenResultPredicate = Preconditions.checkNotNull(value, "Result predicate must be set");
                return this;
            }

            @Override
            public RetryBuilder whenException(Predicate<Throwable> value) {
                this.whenExceptionPredicate = Preconditions.checkNotNull(value, "Exception predicate must be set");
                return this;
            }

            @Override
            public RetryBuilder beforeRetry(Runnable value) {
                Preconditions.checkNotNull(value, "Before retry handler must be set");
                this.beforeRetry = ignored -> value.run();
                return this;
            }

            @Override
            public RetryBuilder beforeRetry(Consumer<Throwable> value) {
                this.beforeRetry = Preconditions.checkNotNull(value, "Before retry handler must be set");
                return this;
            }

            @Override
            public ExponentialBackoffBuilder withExponentialBackoff() {
                return new ExponentialBackoffBuilderImpl(this);
            }

            @Override
            public FibonacciBackoffBuilder withFibonacciBackoff() {
                return new FibonacciBackoffBuilderImpl(this);
            }

            @Override
            public CustomBackoffBuilder withCustomBackoff() {
                return new CustomBackoffBuilderImpl(this);
            }

            @Override
            public RetryBuilder onRetry(Runnable callback) {
                this.onRetry = Preconditions.checkNotNull(callback, "Retry callback must be set");
                return this;
            }

            @Override
            public RetryBuilder onSuccess(Runnable callback) {
                this.onSuccess = Preconditions.checkNotNull(callback, "Success callback must be set");
                return this;
            }

            @Override
            public RetryBuilder onFailure(Runnable callback) {
                this.onFailure = Preconditions.checkNotNull(callback, "Failure callback must be set");
                return this;
            }

            @Override
            public Builder done() {
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

            static class ExponentialBackoffBuilderImpl implements ExponentialBackoffBuilder {
                private final RetryBuilderImpl parent;

                private int factor = 2;
                private long maxDelayInMillis = 60_000;

                ExponentialBackoffBuilderImpl(RetryBuilderImpl parent) {
                    this.parent = parent;
                }

                @Override
                public ExponentialBackoffBuilder factor(int value) {
                    this.factor = Preconditions.check(value, value >= 1, "Factor must be >= 1");
                    return this;
                }

                @Override
                public ExponentialBackoffBuilder maxDelay(long value, ChronoUnit unit) {
                    Preconditions.check(value, value >= 0, "Max delay must be >= 0");
                    Preconditions.checkNotNull(unit, "Max delay unit must be set");

                    this.maxDelayInMillis = timeInMillis(value, unit);
                    return this;
                }

                @Override
                public RetryBuilder done() {
                    parent.exponentialBackoffBuilder = this;
                    return parent;
                }
            }

            static class FibonacciBackoffBuilderImpl implements FibonacciBackoffBuilder {
                private final RetryBuilderImpl parent;

                private long maxDelayInMillis = 60_000;

                FibonacciBackoffBuilderImpl(RetryBuilderImpl parent) {
                    this.parent = parent;
                }

                @Override
                public FibonacciBackoffBuilder maxDelay(long value, ChronoUnit unit) {
                    Preconditions.check(value, value >= 0, "Max delay must be >= 0");
                    Preconditions.checkNotNull(unit, "Max delay unit must be set");

                    this.maxDelayInMillis = timeInMillis(value, unit);
                    return this;
                }

                @Override
                public RetryBuilder done() {
                    parent.fibonacciBackoffBuilder = this;
                    return parent;
                }
            }

            static class CustomBackoffBuilderImpl implements CustomBackoffBuilder {
                private final RetryBuilderImpl parent;

                private Supplier<CustomBackoffStrategy> strategy;

                CustomBackoffBuilderImpl(RetryBuilderImpl parent) {
                    this.parent = parent;
                }

                @Override
                public CustomBackoffBuilder strategy(Supplier<CustomBackoffStrategy> value) {
                    this.strategy = Preconditions.checkNotNull(value, "Custom backoff strategy must be set");
                    return this;
                }

                @Override
                public RetryBuilder done() {
                    Preconditions.checkNotNull(strategy, "Custom backoff strategy must be set");

                    parent.customBackoffBuilder = this;
                    return parent;
                }
            }
        }

        static class TimeoutBuilderImpl implements TimeoutBuilder {
            private final BuilderImpl parent;

            private long durationInMillis = 1000;

            private Runnable onTimeout;
            private Runnable onFinished;

            TimeoutBuilderImpl(BuilderImpl parent) {
                this.parent = parent;
            }

            @Override
            public TimeoutBuilder duration(long value, ChronoUnit unit) {
                Preconditions.check(value, value >= 0, "Timeout duration must be >= 0");
                Preconditions.checkNotNull(unit, "Timeout duration unit must be set");

                this.durationInMillis = timeInMillis(value, unit);
                return this;
            }

            @Override
            public TimeoutBuilder onTimeout(Runnable callback) {
                this.onTimeout = Preconditions.checkNotNull(callback, "Timeout callback must be set");
                return this;
            }

            @Override
            public TimeoutBuilder onFinished(Runnable callback) {
                this.onFinished = Preconditions.checkNotNull(callback, "Finished callback must be set");
                return this;
            }

            @Override
            public Builder done() {
                parent.timeoutBuilder = this;
                return parent;
            }
        }
    }
}
