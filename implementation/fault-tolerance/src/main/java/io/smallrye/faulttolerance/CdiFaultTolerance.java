package io.smallrye.faulttolerance;

import static io.smallrye.faulttolerance.core.Invocation.invocation;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.faulttolerance.api.CustomBackoffStrategy;
import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.async.CompletionStageExecution;
import io.smallrye.faulttolerance.core.async.RememberEventLoop;
import io.smallrye.faulttolerance.core.bulkhead.CompletionStageThreadPoolBulkhead;
import io.smallrye.faulttolerance.core.bulkhead.SemaphoreBulkhead;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreaker;
import io.smallrye.faulttolerance.core.circuit.breaker.CompletionStageCircuitBreaker;
import io.smallrye.faulttolerance.core.event.loop.EventLoop;
import io.smallrye.faulttolerance.core.fallback.CompletionStageFallback;
import io.smallrye.faulttolerance.core.fallback.Fallback;
import io.smallrye.faulttolerance.core.fallback.FallbackFunction;
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
import io.smallrye.faulttolerance.core.timer.Timer;
import io.smallrye.faulttolerance.core.util.DirectExecutor;
import io.smallrye.faulttolerance.core.util.ExceptionDecision;
import io.smallrye.faulttolerance.core.util.Preconditions;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;

// this is mostly a copy of StandaloneFaultTolerance and they must be kept in sync
class CdiFaultTolerance<T> implements FaultTolerance<T> {
    private final FaultToleranceStrategy<T> strategy;

    CdiFaultTolerance(FaultToleranceStrategy<T> strategy) {
        this.strategy = strategy;
    }

    @Override
    public T call(Callable<T> action) throws Exception {
        InvocationContext<T> ctx = new InvocationContext<>(action);
        return strategy.apply(ctx);
    }

    static class BuilderImpl<T, R> implements Builder<T, R> {
        private final boolean ftEnabled;
        private final Executor executor;
        private final Timer timer;
        private final EventLoop eventLoop;
        private final CircuitBreakerMaintenanceImpl cbMaintenance;
        private final boolean isAsync;
        private final Function<FaultTolerance<T>, R> finisher;

        private BulkheadBuilderImpl<T, R> bulkheadBuilder;
        private CircuitBreakerBuilderImpl<T, R> circuitBreakerBuilder;
        private FallbackBuilderImpl<T, R> fallbackBuilder;
        private RetryBuilderImpl<T, R> retryBuilder;
        private TimeoutBuilderImpl<T, R> timeoutBuilder;
        private boolean offloadToAnotherThread;

        BuilderImpl(boolean ftEnabled, Executor executor, Timer timer, EventLoop eventLoop,
                CircuitBreakerMaintenanceImpl cbMaintenance, boolean isAsync,
                Function<FaultTolerance<T>, R> finisher) {
            this.ftEnabled = ftEnabled;
            this.executor = executor;
            this.timer = timer;
            this.eventLoop = eventLoop;
            this.cbMaintenance = cbMaintenance;
            this.isAsync = isAsync;
            this.finisher = finisher;
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
            FaultTolerance<T> result;
            if (isAsync) {
                result = new CdiFaultTolerance(buildAsyncStrategy());
            } else {
                result = new CdiFaultTolerance<>(buildSyncStrategy());
            }
            return finisher.apply(result);
        }

        private FaultToleranceStrategy<T> buildSyncStrategy() {
            FaultToleranceStrategy<T> result = invocation();

            if (ftEnabled && bulkheadBuilder != null) {
                result = new SemaphoreBulkhead<>(result, "unknown", bulkheadBuilder.limit);
            }

            if (ftEnabled && timeoutBuilder != null) {
                result = new Timeout<>(result, "unknown", timeoutBuilder.durationInMillis,
                        new TimerTimeoutWatcher(timer));
            }

            if (ftEnabled && circuitBreakerBuilder != null) {
                result = new CircuitBreaker<>(result, "unknown",
                        createExceptionDecision(circuitBreakerBuilder.skipOn, circuitBreakerBuilder.failOn),
                        circuitBreakerBuilder.delayInMillis,
                        circuitBreakerBuilder.requestVolumeThreshold,
                        circuitBreakerBuilder.failureRatio,
                        circuitBreakerBuilder.successThreshold,
                        new SystemStopwatch());

                String cbName = circuitBreakerBuilder.name != null ? circuitBreakerBuilder.name : UUID.randomUUID().toString();
                cbMaintenance.register(cbName, (CircuitBreaker<?>) result);
            }

            if (ftEnabled && retryBuilder != null) {
                Supplier<BackOff> backoff = prepareRetryBackoff(retryBuilder);

                result = new Retry<>(result, "unknown",
                        createExceptionDecision(retryBuilder.abortOn, retryBuilder.retryOn), retryBuilder.maxRetries,
                        retryBuilder.maxDurationInMillis, () -> new ThreadSleepDelay(backoff.get()),
                        new SystemStopwatch());
            }

            // fallback is always enabled
            if (fallbackBuilder != null) {
                FallbackFunction<T> fallbackFunction = ctx -> fallbackBuilder.handler.apply(ctx.failure);
                result = new Fallback<>(result, "unknown", fallbackFunction,
                        createExceptionDecision(fallbackBuilder.skipOn, fallbackBuilder.applyOn));
            }

            return result;
        }

        private FaultToleranceStrategy<CompletionStage<T>> buildAsyncStrategy() {
            FaultToleranceStrategy<CompletionStage<T>> result = invocation();

            // thread offload is always enabled
            Executor executor = offloadToAnotherThread ? this.executor : DirectExecutor.INSTANCE;
            result = new CompletionStageExecution<>(result, executor);

            if (ftEnabled && bulkheadBuilder != null) {
                result = new CompletionStageThreadPoolBulkhead<>(result, "unknown", bulkheadBuilder.limit,
                        bulkheadBuilder.queueSize);
            }

            if (ftEnabled && timeoutBuilder != null) {
                result = new CompletionStageTimeout<>(result, "unknown", timeoutBuilder.durationInMillis,
                        new TimerTimeoutWatcher(timer));
            }

            if (ftEnabled && circuitBreakerBuilder != null) {
                result = new CompletionStageCircuitBreaker<>(result, "unknown",
                        createExceptionDecision(circuitBreakerBuilder.skipOn, circuitBreakerBuilder.failOn),
                        circuitBreakerBuilder.delayInMillis,
                        circuitBreakerBuilder.requestVolumeThreshold,
                        circuitBreakerBuilder.failureRatio,
                        circuitBreakerBuilder.successThreshold,
                        new SystemStopwatch());

                String cbName = circuitBreakerBuilder.name != null ? circuitBreakerBuilder.name : UUID.randomUUID().toString();
                cbMaintenance.register(cbName, (CircuitBreaker<?>) result);
            }

            if (ftEnabled && retryBuilder != null) {
                Supplier<BackOff> backoff = prepareRetryBackoff(retryBuilder);

                result = new CompletionStageRetry<>(result, "unknown",
                        createExceptionDecision(retryBuilder.abortOn, retryBuilder.retryOn), retryBuilder.maxRetries,
                        retryBuilder.maxDurationInMillis, () -> new TimerDelay(backoff.get(), timer),
                        new SystemStopwatch());
            }

            // fallback is always enabled
            if (fallbackBuilder != null) {
                FallbackFunction<CompletionStage<T>> fallbackFunction = ctx -> (CompletionStage<T>) fallbackBuilder.handler
                        .apply(ctx.failure);
                result = new CompletionStageFallback<>(result, "unknown", fallbackFunction,
                        createExceptionDecision(fallbackBuilder.skipOn, fallbackBuilder.applyOn));
            }

            // thread offload is always enabled
            if (!offloadToAnotherThread) {
                result = new RememberEventLoop<>(result, eventLoop);
            }

            return result;
        }

        private static long getTimeInMs(long time, ChronoUnit unit) {
            return Duration.of(time, unit).toMillis();
        }

        private static ExceptionDecision createExceptionDecision(Collection<Class<? extends Throwable>> consideredExpected,
                Collection<Class<? extends Throwable>> consideredFailure) {
            return new ExceptionDecision(createSetOfThrowables(consideredFailure),
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
            public Builder<T, R> done() {
                parent.bulkheadBuilder = this;
                return parent;
            }
        }

        static class CircuitBreakerBuilderImpl<T, R> implements CircuitBreakerBuilder<T, R> {
            private final BuilderImpl<T, R> parent;

            private Collection<Class<? extends Throwable>> failOn = Collections.singleton(Throwable.class);
            private Collection<Class<? extends Throwable>> skipOn = Collections.emptySet();
            private long delayInMillis = 5000;
            private int requestVolumeThreshold = 20;
            private double failureRatio = 0.5;
            private int successThreshold = 1;

            private String name; // unnamed by default

            CircuitBreakerBuilderImpl(BuilderImpl<T, R> parent) {
                this.parent = parent;
            }

            @Override
            public CircuitBreakerBuilder<T, R> failOn(Collection<Class<? extends Throwable>> value) {
                this.failOn = Preconditions.checkNotNull(value, "Exceptions considered failure must be set");
                return this;
            }

            @Override
            public CircuitBreakerBuilder<T, R> skipOn(Collection<Class<? extends Throwable>> value) {
                this.skipOn = Preconditions.checkNotNull(value, "Exceptions considered success must be set");
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
            public Builder<T, R> done() {
                parent.circuitBreakerBuilder = this;
                return parent;
            }
        }

        static class FallbackBuilderImpl<T, R> implements FallbackBuilder<T, R> {
            private final BuilderImpl<T, R> parent;

            private Function<Throwable, T> handler;
            private Collection<Class<? extends Throwable>> applyOn = Collections.singleton(Throwable.class);
            private Collection<Class<? extends Throwable>> skipOn = Collections.emptySet();

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
                return this;
            }

            @Override
            public FallbackBuilder<T, R> skipOn(Collection<Class<? extends Throwable>> value) {
                this.skipOn = Preconditions.checkNotNull(value, "Exceptions to skip fallback on must be set");
                return this;
            }

            @Override
            public Builder<T, R> done() {
                Preconditions.checkNotNull(handler, "Fallback handler must be set");

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

            private ExponentialBackoffBuilderImpl<T, R> exponentialBackoffBuilder;
            private FibonacciBackoffBuilderImpl<T, R> fibonacciBackoffBuilder;
            private CustomBackoffBuilderImpl<T, R> customBackoffBuilder;

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
                return this;
            }

            @Override
            public RetryBuilder<T, R> abortOn(Collection<Class<? extends Throwable>> value) {
                this.abortOn = Preconditions.checkNotNull(value, "Exceptions to abort retrying on must be set");
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
            public Builder<T, R> done() {
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
            public Builder<T, R> done() {
                parent.timeoutBuilder = this;
                return parent;
            }
        }
    }
}
