package io.smallrye.faulttolerance.api;

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.enterprise.util.TypeLiteral;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import io.smallrye.common.annotation.Experimental;

/**
 * Allows guarding an action with various fault tolerance strategies: bulkhead, circuit breaker, fallback, rate limit,
 * retry. and timeout. Synchronous as well as asynchronous actions may be guarded, asynchronous actions may optionally
 * be offloaded to another thread.
 * <p>
 * An instance of this interface represents a configured set of fault tolerance strategies. It can be used to
 * guard a {@link #call(Callable) Callable} or {@link #get(Supplier) Supplier} invocation, or adapt an unguarded
 * {@link #adaptCallable(Callable) Callable} or {@link #adaptSupplier(Supplier) Supplier} to a guarded one.
 * <p>
 * The {@link #create(Class)} methods return a builder that allows configuring the supported fault tolerance strategies.
 * Order of builder method invocations does not matter, the fault tolerance strategies are always applied
 * in a predefined order: fallback &gt; retry &gt; circuit breaker &gt; rate limit &gt; timeout &gt; bulkhead &gt;
 * thread offload &gt; guarded action.
 * <p>
 * Note that bulkheads, circuit breakers and rate limits are stateful, so there's a big difference between guarding
 * multiple actions using the same {@code TypedGuard} object and using a separate {@code TypedGuard} object for each
 * action. Using a single {@code TypedGuard} instance to guard multiple actions means that a single bulkhead,
 * circuit breaker and/or rate limit will be shared among all those actions.
 * <p>
 * This API is essentially a programmatic equivalent to the declarative, annotation-based API of MicroProfile Fault
 * Tolerance and SmallRye Fault Tolerance. It shares the set of fault tolerance strategies, their invocation order
 * and behavior, their configuration properties, etc. Notable differences are:
 * <ul>
 * <li>asynchronous actions of type {@link java.util.concurrent.Future} are not supported;</li>
 * <li>the fallback, circuit breaker and retry strategies always inspect the cause chain of exceptions,
 * following the behavior of SmallRye Fault Tolerance in the non-compatible mode.</li>
 * </ul>
 *
 * @param <T> type of value of the guarded action
 */
@Experimental("second attempt at providing programmatic API")
public interface TypedGuard<T> {
    /**
     * Creates a builder for producing a {@link TypedGuard} object representing a set of configured
     * fault tolerance strategies that guard given {@code type}. It can be used to execute actions using
     * {@link #call(Callable)} or {@link #get(Supplier)}.
     * <p>
     * The given {@code type} is also used to determine if the guarded actions are synchronous or asynchronous.
     * Casting to another type is not possible.
     */
    static <T> Builder<T> create(Class<T> type) {
        return SpiAccess.get().newTypedGuardBuilder(type);
    }

    /**
     * Creates a builder for producing a {@link TypedGuard} object representing a set of configured
     * fault tolerance strategies that guard given {@code type}. It can be used to execute actions using
     * {@link #call(Callable) call()} or {@link #get(Supplier) get()}.
     * <p>
     * The given {@code type} is also used to determine if the guarded actions are synchronous or asynchronous.
     */
    static <T> Builder<T> create(TypeLiteral<T> type) {
        return SpiAccess.get().newTypedGuardBuilder(type.getType());
    }

    /**
     * Calls given {@code action} and guards the call by this configured set of fault tolerance strategies.
     * <p>
     * If this {@code TypedGuard} instance was created using a synchronous type, the action is synchronous
     * and is always executed on the same thread that calls this method. If this {@code TypedGuard} instance
     * was created using an asynchronous type, the action is asynchronous and may be offloaded to another thread
     * depending on how the builder was configured.
     */
    T call(Callable<T> action) throws Exception;

    /**
     * Calls given {@code action} and guards the call by this configured set of fault tolerance strategies.
     * <p>
     * If this {@code TypedGuard} instance was created using a synchronous type, the action is synchronous
     * and is always executed on the same thread that calls this method. If this {@code TypedGuard} instance
     * was created using an asynchronous type, the action is asynchronous and may be offloaded to another thread
     * depending on how the builder was configured.
     */
    T get(Supplier<T> action);

    /**
     * Adapts given {@code action} to an action guarded by this configured set of fault tolerance strategies.
     * Useful when the action has to be called multiple times.
     * <p>
     * Equivalent to {@code () -> call(action)}.
     *
     * @see #call(Callable)
     */
    default Callable<T> adaptCallable(Callable<T> action) {
        return () -> call(action);
    }

    /**
     * Adapts given {@code action} to an action guarded by this configured set of fault tolerance strategies.
     * Useful when the action has to be called multiple times.
     * <p>
     * Equivalent to {@code () -> get(action)}.
     *
     * @see #get(Supplier)
     */
    default Supplier<T> adaptSupplier(Supplier<T> action) {
        return () -> get(action);
    }

    /**
     * A builder for configuring fault tolerance strategies. A fault tolerance strategy is included in the resulting
     * set if the corresponding {@code with[Strategy]} method is called. Each strategy has its own builder to configure
     * the necessary attributes, and each such builder has a {@code done()} method that returns back to this builder.
     * <p>
     * In general, all builders in this API accept multiple invocations of the same method, but only the last
     * invocation is meaningful. Any previous invocations are forgotten.
     *
     * @param <T> type of value of the guarded action
     */
    interface Builder<T> {
        /**
         * Assigns a description to the resulting set of configured fault tolerance strategies. The description
         * is used in logging messages and exception messages, and also as an identifier for metrics.
         * <p>
         * The description may be an arbitrary string. Duplicates are permitted.
         * <p>
         * If no description is set, no metrics are emitted and a random UUID is used for other purposes.
         *
         * @param value a description, must not be {@code null}
         * @return this fault tolerance builder
         */
        Builder<T> withDescription(String value);

        /**
         * Adds a bulkhead strategy. In this API, bulkhead is a simple concurrency limiter.
         *
         * @return a builder to configure the bulkhead strategy
         * @see Bulkhead @Bulkhead
         */
        BulkheadBuilder<T> withBulkhead();

        /**
         * Adds a circuit breaker strategy.
         *
         * @return a builder to configure the circuit breaker strategy
         * @see CircuitBreaker @CircuitBreaker
         */
        CircuitBreakerBuilder<T> withCircuitBreaker();

        /**
         * Adds a fallback strategy.
         *
         * @return a builder to configure the fallback strategy
         * @see Fallback @Fallback
         */
        FallbackBuilder<T> withFallback();

        /**
         * Adds a rate limit strategy.
         *
         * @return a builder to configure the rate limit strategy
         * @see RateLimit @RateLimit
         */
        RateLimitBuilder<T> withRateLimit();

        /**
         * Adds a retry strategy. Retry uses constant backoff between attempts by default,
         * but may be configured to use exponential backoff, Fibonacci backoff, or custom backoff.
         *
         * @return a builder to configure the retry strategy
         * @see Retry @Retry
         */
        RetryBuilder<T> withRetry();

        /**
         * Adds a timeout strategy.
         *
         * @return a builder to configure the timeout strategy
         * @see Timeout @Timeout
         */
        TimeoutBuilder<T> withTimeout();

        /**
         * Configures whether an asynchronous guarded action should be offloaded to another thread.
         *
         * @param value whether an asynchronous guarded action should be offloaded to another thread
         * @return this fault tolerance builder
         * @see Asynchronous @Asynchronous
         * @see AsynchronousNonBlocking @AsynchronousNonBlocking
         */
        Builder<T> withThreadOffload(boolean value);

        /**
         * Configures the executor to use when offloading the guarded action to another thread.
         * <p>
         * If this method is not called but thread offload is enabled using {@link #withThreadOffload(boolean)},
         * an asynchronous guarded action is offloaded to the default executor provided by the integrator.
         *
         * @param executor the executor to which the guarded action should be offloaded
         * @return this fault tolerance builder
         */
        Builder<T> withThreadOffloadExecutor(Executor executor);

        /**
         * Returns a ready-to-use instance of {@code TypedGuard}.
         */
        TypedGuard<T> build();

        /**
         * Syntactic sugar for calling the builder methods conditionally without breaking the invocation chain.
         * For example:
         *
         * <!-- @formatter:off -->
         * <pre>{@code
         * TypedGuard.create(...)
         *     .withRetry() ... .done()
         *     .with(builder -> {
         *         if (useTimeout) {
         *             builder.withTimeout() ... .done();
         *         }
         *     })
         *     .build();
         * }</pre>
         * <!-- @formatter:on -->
         *
         * @param consumer block of code to execute with this builder
         * @return this fault tolerance builder
         */
        default Builder<T> with(Consumer<Builder<T>> consumer) {
            consumer.accept(this);
            return this;
        }

        /**
         * Configures a bulkhead.
         *
         * @see Bulkhead @Bulkhead
         */
        interface BulkheadBuilder<T> {
            /**
             * Sets the concurrency limit the bulkhead will enforce. Defaults to 10.
             *
             * @param value the concurrency limit, must be &gt;= 1
             * @return this bulkhead builder
             * @see Bulkhead#value() @Bulkhead.value
             */
            BulkheadBuilder<T> limit(int value);

            /**
             * Sets the maximum size of the bulkhead queue. Defaults to 10.
             *
             * @param value the queue size, must be &gt;= 1
             * @return this bulkhead builder
             * @see Bulkhead#waitingTaskQueue() @Bulkhead.waitingTaskQueue
             */
            BulkheadBuilder<T> queueSize(int value);

            /**
             * Enables bulkhead queueing for synchronous actions.
             * <p>
             * If you use this method, you <strong>have to ensure</strong> that the guard
             * is executed on a virtual thread.
             *
             * @return this bulkhead builder
             */
            BulkheadBuilder<T> enableSynchronousQueueing();

            /**
             * Sets a callback that will be invoked when this bulkhead accepts an invocation.
             * In case of asynchronous actions, accepting into bulkhead doesn't mean the action
             * is immediately invoked; the invocation is first put into a queue and may wait there.
             * <p>
             * The callback must be fast and non-blocking and must not throw an exception.
             *
             * @param callback the accepted callback, must not be {@code null}
             * @return this bulkhead builder
             */
            BulkheadBuilder<T> onAccepted(Runnable callback);

            /**
             * Sets a callback that will be invoked when this bulkhead rejects an invocation.
             * <p>
             * The callback must be fast and non-blocking and must not throw an exception.
             *
             * @param callback the rejected callback, must not be {@code null}
             * @return this bulkhead builder
             */
            BulkheadBuilder<T> onRejected(Runnable callback);

            /**
             * Sets a callback that will be invoked when a finished invocation leaves this bulkhead.
             * <p>
             * The callback must be fast and non-blocking and must not throw an exception.
             *
             * @param callback the finished callback, must not be {@code null}
             * @return this bulkhead builder
             */
            BulkheadBuilder<T> onFinished(Runnable callback);

            /**
             * Returns the original fault tolerance builder.
             *
             * @return the original fault tolerance builder
             */
            Builder<T> done();

            default BulkheadBuilder<T> with(Consumer<BulkheadBuilder<T>> consumer) {
                consumer.accept(this);
                return this;
            }
        }

        /**
         * Configures a circuit breaker.
         *
         * @see CircuitBreaker @CircuitBreaker
         */
        interface CircuitBreakerBuilder<T> {
            /**
             * Sets the set of exception types considered failure. Defaults to all exceptions ({@code Throwable}).
             *
             * @param value collection of exception types, must not be {@code null}
             * @return this circuit breaker builder
             * @see CircuitBreaker#failOn() @CircuitBreaker.failOn
             */
            CircuitBreakerBuilder<T> failOn(Collection<Class<? extends Throwable>> value);

            /**
             * Equivalent to {@link #failOn(Collection) failOn(Set.of(value))}.
             *
             * @param value an exception class, must not be {@code null}
             * @return this circuit breaker builder
             */
            CircuitBreakerBuilder<T> failOn(Class<? extends Throwable> value);

            /**
             * Sets the set of exception types considered success. Defaults to no exception (empty set).
             *
             * @param value collection of exception types, must not be {@code null}
             * @return this circuit breaker builder
             * @see CircuitBreaker#skipOn() @CircuitBreaker.skipOn
             */
            CircuitBreakerBuilder<T> skipOn(Collection<Class<? extends Throwable>> value);

            /**
             * Equivalent to {@link #skipOn(Collection) skipOn(Set.of(value))}.
             *
             * @param value an exception class, must not be {@code null}
             * @return this circuit breaker builder
             */
            CircuitBreakerBuilder<T> skipOn(Class<? extends Throwable> value);

            /**
             * Sets a predicate to determine when an exception should be considered failure
             * by the circuit breaker. This is a more general variant of {@link #failOn(Collection) failOn}.
             * Note that there is no generalized {@link #skipOn(Collection) skipOn}, because all exceptions
             * that do not match this predicate are implicitly considered success.
             * <p>
             * If this method is called, {@code failOn} and {@code skipOn} may not be called.
             *
             * @param value the predicate, must not be {@code null}
             * @return this circuit breaker builder
             */
            CircuitBreakerBuilder<T> when(Predicate<Throwable> value);

            /**
             * Sets the delay after which an open circuit moves to half-open. Defaults to 5 seconds.
             *
             * @param value the delay length, must be &gt;= 0
             * @param unit the delay unit, must not be {@code null}
             * @return this circuit breaker builder
             * @see CircuitBreaker#delay() @CircuitBreaker.delay
             * @see CircuitBreaker#delayUnit() @CircuitBreaker.delayUnit
             */
            CircuitBreakerBuilder<T> delay(long value, ChronoUnit unit);

            /**
             * Sets the size of the circuit breaker's rolling window.
             *
             * @param value the size of the circuit breaker's rolling window, must be &gt;= 1
             * @return this circuit breaker builder
             * @see CircuitBreaker#requestVolumeThreshold() @CircuitBreaker.requestVolumeThreshold
             */
            CircuitBreakerBuilder<T> requestVolumeThreshold(int value);

            /**
             * Sets the failure ratio that, once reached, will move a closed circuit breaker to open. Defaults to 0.5.
             *
             * @param value the failure ratio, must be &gt;= 0 and &lt;= 1
             * @return this circuit breaker builder
             * @see CircuitBreaker#failureRatio() @CircuitBreaker.failureRatio
             */
            CircuitBreakerBuilder<T> failureRatio(double value);

            /**
             * Sets the number of successful executions that, once reached, will move a half-open circuit breaker
             * to closed. Defaults to 1.
             *
             * @param value the number of successful executions, must be &gt;= 1
             * @return this circuit breaker builder
             * @see CircuitBreaker#successThreshold() @CircuitBreaker.successThreshold
             */
            CircuitBreakerBuilder<T> successThreshold(int value);

            /**
             * Sets a circuit breaker name. Required to use the {@link CircuitBreakerMaintenance} methods.
             * Defaults to unnamed. It is an error to use the same name for multiple circuit breakers.
             * <p>
             * If a circuit breaker is not given a name, its state will <em>not</em> be affected
             * by {@link CircuitBreakerMaintenance#resetAll()}. This is unlike unnamed circuit breakers
             * declared using {@code @CircuitBreaker}, because there's a fixed number of circuit breakers
             * created using the declarative API, but a potentially unbounded number of circuit breakers
             * created using the programmatic API. In other words, automatically remembering all
             * circuit breakers created using the programmatic API would easily lead to a memory leak.
             *
             * @param value the circuit breaker name, must not be {@code null}
             * @return this circuit breaker builder
             * @see CircuitBreakerName @CircuitBreakerName
             */
            CircuitBreakerBuilder<T> name(String value);

            /**
             * Sets a callback that will be invoked upon each state change of this circuit breaker.
             * <p>
             * The callback must be fast and non-blocking and must not throw an exception.
             *
             * @param callback the state change callback, must not be {@code null}
             * @return this circuit breaker builder
             */
            CircuitBreakerBuilder<T> onStateChange(Consumer<CircuitBreakerState> callback);

            /**
             * Sets a callback that will be invoked when this circuit breaker treats a finished invocation as success.
             * <p>
             * The callback must be fast and non-blocking and must not throw an exception.
             *
             * @param callback the success callback, must not be {@code null}
             * @return this circuit breaker builder
             */
            CircuitBreakerBuilder<T> onSuccess(Runnable callback);

            /**
             * Sets a callback that will be invoked when this circuit breaker treats a finished invocation as failure.
             * <p>
             * The callback must be fast and non-blocking and must not throw an exception.
             *
             * @param callback the failure callback, must not be {@code null}
             * @return this circuit breaker builder
             */
            CircuitBreakerBuilder<T> onFailure(Runnable callback);

            /**
             * Sets a callback that will be invoked when this circuit breaker prevents an invocation, because it is
             * in the open or half-open state.
             * <p>
             * The callback must be fast and non-blocking and must not throw an exception.
             *
             * @param callback the prevented callback, must not be {@code null}
             * @return this circuit breaker builder
             */
            CircuitBreakerBuilder<T> onPrevented(Runnable callback);

            /**
             * Returns the original fault tolerance builder.
             *
             * @return the original fault tolerance builder
             */
            Builder<T> done();

            default CircuitBreakerBuilder<T> with(Consumer<CircuitBreakerBuilder<T>> consumer) {
                consumer.accept(this);
                return this;
            }
        }

        /**
         * Configures a fallback.
         *
         * @see Fallback @Fallback
         */
        interface FallbackBuilder<T> {
            /**
             * Sets the fallback handler in the form of a fallback value {@link Supplier}.
             *
             * @param value the fallback value supplier, must not be {@code null}
             * @return this fallback builder
             */
            FallbackBuilder<T> handler(Supplier<T> value);

            /**
             * Sets the fallback handler in the form of a {@link Function} that transforms the exception
             * to the fallback value.
             *
             * @param value the fallback value function, must not be {@code null}
             * @return this fallback builder
             */
            FallbackBuilder<T> handler(Function<Throwable, T> value);

            /**
             * Sets the set of exception types considered failure. Defaults to all exceptions ({@code Throwable}).
             *
             * @param value collection of exception types, must not be {@code null}
             * @return this fallback builder
             * @see Fallback#applyOn() @Fallback.applyOn
             */
            FallbackBuilder<T> applyOn(Collection<Class<? extends Throwable>> value);

            /**
             * Equivalent to {@link #applyOn(Collection) applyOn(Set.of(value))}.
             *
             * @param value an exception class, must not be {@code null}
             * @return this fallback builder
             */
            FallbackBuilder<T> applyOn(Class<? extends Throwable> value);

            /**
             * Sets the set of exception types considered success. Defaults to no exception (empty set).
             *
             * @param value collection of exception types, must not be {@code null}
             * @return this fallback builder
             * @see Fallback#skipOn() @Fallback.skipOn
             */
            FallbackBuilder<T> skipOn(Collection<Class<? extends Throwable>> value);

            /**
             * Equivalent to {@link #skipOn(Collection) skipOn(Set.of(value))}.
             *
             * @param value an exception class, must not be {@code null}
             * @return this fallback builder
             */
            FallbackBuilder<T> skipOn(Class<? extends Throwable> value);

            /**
             * Sets a predicate to determine when an exception should be considered failure
             * and fallback should be applied. This is a more general variant of {@link #applyOn(Collection) applyOn}.
             * Note that there is no generalized {@link #skipOn(Collection) skipOn}, because all exceptions
             * that do not match this predicate are implicitly considered success.
             * <p>
             * If this method is called, {@code applyOn} and {@code skipOn} may not be called.
             *
             * @param value the predicate, must not be {@code null}
             * @return this fallback builder
             */
            FallbackBuilder<T> when(Predicate<Throwable> value);

            /**
             * Returns the original fault tolerance builder.
             *
             * @return the original fault tolerance builder
             */
            Builder<T> done();

            default FallbackBuilder<T> with(Consumer<FallbackBuilder<T>> consumer) {
                consumer.accept(this);
                return this;
            }
        }

        /**
         * Configures a rate limit.
         *
         * @see RateLimit @RateLimit
         */
        interface RateLimitBuilder<T> {
            /**
             * Sets the maximum number of invocations in a time window. Defaults to 100.
             *
             * @param value maximum number of invocations in a time window, must be &gt;= 1
             * @return this rate limit builder
             * @see RateLimit#value() @RateLimit.value
             */
            RateLimitBuilder<T> limit(int value);

            /**
             * Sets the time window length. Defaults to 1 second.
             *
             * @param value the time window size, must be &gt;= 1
             * @return this rate limit builder
             * @see RateLimit#window() @RateLimit.window
             * @see RateLimit#windowUnit() @RateLimit.windowUnit
             */
            RateLimitBuilder<T> window(long value, ChronoUnit unit);

            /**
             * Sets the minimum spacing between invocations. Defaults to 0.
             *
             * @param value the minimum spacing, must be &gt;= 0
             * @return this rate limit builder
             * @see RateLimit#minSpacing() @RateLimit.minSpacing
             * @see RateLimit#minSpacingUnit() @RateLimit.minSpacingUnit
             */
            RateLimitBuilder<T> minSpacing(long value, ChronoUnit unit);

            /**
             * Sets the type of time windows used for rate limiting. Defaults to {@link RateLimitType#FIXED}.
             *
             * @param value the time window type, must not be {@code null}
             * @return this rate limit builder
             * @see RateLimit#type() @RateLimit.type
             */
            RateLimitBuilder<T> type(RateLimitType value);

            /**
             * Sets a callback that will be invoked when this rate limit permits an invocation.
             * <p>
             * The callback must be fast and non-blocking and must not throw an exception.
             *
             * @param callback the permitted callback, must not be {@code null}
             * @return this rate limit builder
             */
            RateLimitBuilder<T> onPermitted(Runnable callback);

            /**
             * Sets a callback that will be invoked when this rate limit rejects an invocation.
             * <p>
             * The callback must be fast and non-blocking and must not throw an exception.
             *
             * @param callback the rejected callback, must not be {@code null}
             * @return this rate limit builder
             */
            RateLimitBuilder<T> onRejected(Runnable callback);

            /**
             * Returns the original fault tolerance builder.
             *
             * @return the original fault tolerance builder
             */
            Builder<T> done();

            default RateLimitBuilder<T> with(Consumer<RateLimitBuilder<T>> consumer) {
                consumer.accept(this);
                return this;
            }
        }

        /**
         * Configures a retry.
         *
         * @see Retry @Retry
         */
        interface RetryBuilder<T> {
            /**
             * Sets the maximum number of retries. Defaults to 3.
             *
             * @param value the maximum number of retries, must be &gt;= -1.
             * @return this retry builder
             * @see Retry#maxRetries() @Retry.maxRetries
             */
            RetryBuilder<T> maxRetries(int value);

            /**
             * Sets the delay between retries. Defaults to 0.
             *
             * @param value the delay length, must be &gt;= 0
             * @param unit the delay unit, must not be {@code null}
             * @return this retry builder
             * @see Retry#delay() @Retry.delay
             * @see Retry#delayUnit() @Retry.delayUnit
             */
            RetryBuilder<T> delay(long value, ChronoUnit unit);

            /**
             * Sets the maximum duration of all invocations, including possible retries. Defaults to 3 minutes.
             *
             * @param value the maximum duration length, must be &gt;= 0
             * @param unit the maximum duration unit, must not be {@code null}
             * @return this retry builder
             * @see Retry#maxDuration() @Retry.maxDuration
             * @see Retry#durationUnit() @Retry.durationUnit
             */
            RetryBuilder<T> maxDuration(long value, ChronoUnit unit);

            /**
             * Sets the jitter bound. Random value in the range from {@code -jitter} to {@code +jitter} will be added
             * to the delay between retry attempts. Defaults to 200 millis.
             *
             * @param value the jitter bound length, must be &gt;= 0
             * @param unit the jitter bound unit, must not be {@code null}
             * @return this retry builder
             * @see Retry#jitter() @Retry.jitter
             * @see Retry#jitterDelayUnit() @Retry.jitterDelayUnit
             */
            RetryBuilder<T> jitter(long value, ChronoUnit unit);

            /**
             * Sets the set of exception types considered failure. Defaults to all exceptions ({@code Exception}).
             *
             * @param value collection of exception types, must not be {@code null}
             * @return this retry builder
             * @see Retry#retryOn() @Retry.retryOn
             */
            RetryBuilder<T> retryOn(Collection<Class<? extends Throwable>> value);

            /**
             * Equivalent to {@link #retryOn(Collection) retryOn(Set.of(value))}.
             *
             * @param value an exception class, must not be {@code null}
             * @return this retry builder
             */
            RetryBuilder<T> retryOn(Class<? extends Throwable> value);

            /**
             * Sets the set of exception types considered success. Defaults to no exception (empty set).
             *
             * @param value collection of exception types, must not be {@code null}
             * @return this retry builder
             * @see Retry#abortOn() @Retry.abortOn
             */
            RetryBuilder<T> abortOn(Collection<Class<? extends Throwable>> value);

            /**
             * Equivalent to {@link #abortOn(Collection) abortOn(Set.of(value))}.
             *
             * @param value an exception class, must not be {@code null}
             * @return this retry builder
             */
            RetryBuilder<T> abortOn(Class<? extends Throwable> value);

            /**
             * Sets a predicate to determine when a result should be considered failure and retry
             * should be attempted. All results that do not match this predicate are implicitly
             * considered success.
             *
             * @param value the predicate, must not be {@code null}
             * @return this retry builder
             */
            RetryBuilder<T> whenResult(Predicate<Object> value);

            /**
             * Sets a predicate to determine when an exception should be considered failure
             * and retry should be attempted. This is a more general variant of {@link #retryOn(Collection) retryOn}.
             * Note that there is no generalized {@link #abortOn(Collection) abortOn}, because all exceptions
             * that do not match this predicate are implicitly considered success.
             * <p>
             * If this method is called, {@code retryOn} and {@code abortOn} may not be called.
             *
             * @param value the predicate, must not be {@code null}
             * @return this retry builder
             * @see BeforeRetry @BeforeRetry
             */
            RetryBuilder<T> whenException(Predicate<Throwable> value);

            /**
             * Sets a before retry handler, which is called before each retry, but not before the original attempt.
             *
             * @param value the before retry handler, must not be {@code null}
             * @return this retry builder
             * @see BeforeRetry @BeforeRetry
             */
            RetryBuilder<T> beforeRetry(Runnable value);

            /**
             * Sets a before retry handler, which is called before each retry, but not before the original attempt.
             *
             * @param value the before retry handler, must not be {@code null}
             * @return this retry builder
             */
            RetryBuilder<T> beforeRetry(Consumer<Throwable> value);

            /**
             * Configures retry to use an exponential backoff instead of the default constant backoff.
             * <p>
             * Only one backoff strategy may be configured, so calling {@link #withFibonacciBackoff()}
             * or {@link #withCustomBackoff()} in addition to this method leads to an exception
             * during {@link #done()}.
             *
             * @return the exponential backoff builder
             * @see ExponentialBackoff @ExponentialBackoff
             */
            ExponentialBackoffBuilder<T> withExponentialBackoff();

            /**
             * Configures retry to use a Fibonacci backoff instead of the default constant backoff.
             * <p>
             * Only one backoff strategy may be configured, so calling {@link #withExponentialBackoff()}
             * or {@link #withCustomBackoff()} in addition to this method leads to an exception
             * during {@link #done()}.
             *
             * @return the Fibonacci backoff builder
             * @see FibonacciBackoff @FibonacciBackoff
             */
            FibonacciBackoffBuilder<T> withFibonacciBackoff();

            /**
             * Configures retry to use a custom backoff instead of the default constant backoff.
             * <p>
             * Only one backoff strategy may be configured, so calling {@link #withExponentialBackoff()}
             * or {@link #withFibonacciBackoff()} in addition to this method leads to an exception
             * during {@link #done()}.
             *
             * @return the custom backoff builder
             * @see CustomBackoff @CustomBackoff
             */
            CustomBackoffBuilder<T> withCustomBackoff();

            /**
             * Sets a callback that will be invoked when a retry is attempted.
             * <p>
             * The callback must be fast and non-blocking and must not throw an exception.
             *
             * @param callback the retried callback, must not be {@code null}
             * @return this retry builder
             */
            RetryBuilder<T> onRetry(Runnable callback);

            /**
             * Sets a callback that will be invoked when this retry strategy treats a finished invocation as success,
             * regardless of whether a retry was attempted or not.
             * <p>
             * The callback must be fast and non-blocking and must not throw an exception.
             *
             * @param callback the retried callback, must not be {@code null}
             * @return this retry builder
             */
            RetryBuilder<T> onSuccess(Runnable callback);

            /**
             * Sets a callback that will be invoked when this retry strategy treats a finished invocation as failure,
             * and no more retries will be attempted. The failure may be caused by depleting the maximum
             * number of retries or the maximum duration, or by an exception that is not retryable.
             * <p>
             * The callback must be fast and non-blocking and must not throw an exception.
             *
             * @param callback the retried callback, must not be {@code null}
             * @return this retry builder
             */
            RetryBuilder<T> onFailure(Runnable callback);

            /**
             * Returns the original fault tolerance builder.
             *
             * @return the original fault tolerance builder
             */
            Builder<T> done();

            default RetryBuilder<T> with(Consumer<RetryBuilder<T>> consumer) {
                consumer.accept(this);
                return this;
            }

            /**
             * Configures an exponential backoff for retry.
             *
             * @see ExponentialBackoff @ExponentialBackoff
             */
            interface ExponentialBackoffBuilder<T> {
                /**
                 * Sets the multiplicative factor used to determine delay between retries. Defaults to 2.
                 *
                 * @param value the multiplicative factor, must be &gt;= 1
                 * @return this exponential backoff builder
                 * @see ExponentialBackoff#factor() @ExponentialBackoff.factor
                 */
                ExponentialBackoffBuilder<T> factor(int value);

                /**
                 * Sets the maximum delay between retries. Defaults to 1 minute.
                 *
                 * @param value the maximum delay, must be &gt;= 0
                 * @param unit the maximum delay unit, must not be {@code null}
                 * @return this exponential backoff builder
                 * @see ExponentialBackoff#maxDelay() @ExponentialBackoff.maxDelay
                 * @see ExponentialBackoff#maxDelayUnit() @ExponentialBackoff.maxDelayUnit
                 */
                ExponentialBackoffBuilder<T> maxDelay(long value, ChronoUnit unit);

                /**
                 * Returns the original retry builder.
                 *
                 * @return the original retry builder
                 */
                RetryBuilder<T> done();

                default ExponentialBackoffBuilder<T> with(Consumer<ExponentialBackoffBuilder<T>> consumer) {
                    consumer.accept(this);
                    return this;
                }
            }

            /**
             * Configures a Fibonacci backoff for retry.
             *
             * @see FibonacciBackoff @FibonacciBackoff
             */
            interface FibonacciBackoffBuilder<T> {
                /**
                 * Sets the maximum delay between retries. Defaults to 1 minute.
                 *
                 * @param value the maximum delay, must be &gt;= 0
                 * @param unit the maximum delay unit, must not be {@code null}
                 * @return this fibonacci backoff builder
                 * @see FibonacciBackoff#maxDelay() @FibonacciBackoff.maxDelay
                 * @see FibonacciBackoff#maxDelayUnit() @FibonacciBackoff.maxDelayUnit
                 */
                FibonacciBackoffBuilder<T> maxDelay(long value, ChronoUnit unit);

                /**
                 * Returns the original retry builder.
                 *
                 * @return the original retry builder
                 */
                RetryBuilder<T> done();

                default FibonacciBackoffBuilder<T> with(Consumer<FibonacciBackoffBuilder<T>> consumer) {
                    consumer.accept(this);
                    return this;
                }
            }

            /**
             * Configures a custom backoff for retry.
             *
             * @see CustomBackoff @CustomBackoff
             */
            interface CustomBackoffBuilder<T> {
                /**
                 * Sets the custom backoff strategy in the form of a {@link Supplier} of {@link CustomBackoffStrategy}
                 * instances. Mandatory.
                 *
                 * @see CustomBackoff#value()
                 */
                CustomBackoffBuilder<T> strategy(Supplier<CustomBackoffStrategy> value);

                /**
                 * Returns the original retry builder.
                 */
                RetryBuilder<T> done();

                default CustomBackoffBuilder<T> with(Consumer<CustomBackoffBuilder<T>> consumer) {
                    consumer.accept(this);
                    return this;
                }
            }
        }

        /**
         * Configures a timeout.
         *
         * @see Timeout @Timeout
         */
        interface TimeoutBuilder<T> {
            /**
             * Sets the timeout duration. Defaults to 1 second.
             *
             * @param value the timeout length, must be &gt;= 0
             * @param unit the timeout unit, must not be {@code null}
             * @return this timeout builder
             * @see Timeout#value() @Timeout.value
             * @see Timeout#unit() @Timeout.unit
             */
            TimeoutBuilder<T> duration(long value, ChronoUnit unit);

            /**
             * Sets a callback that will be invoked when an invocation times out.
             * <p>
             * The callback must be fast and non-blocking and must not throw an exception.
             *
             * @param callback the timeout callback, must not be {@code null}
             * @return this timeout builder
             */
            TimeoutBuilder<T> onTimeout(Runnable callback);

            /**
             * Sets a callback that will be invoked when an invocation finishes before the timeout.
             * <p>
             * The callback must be fast and non-blocking and must not throw an exception.
             *
             * @param callback the finished callback, must not be {@code null}
             * @return this timeout builder
             */
            TimeoutBuilder<T> onFinished(Runnable callback);

            /**
             * Returns the original fault tolerance builder.
             *
             * @return the original fault tolerance builder
             */
            Builder<T> done();

            default TimeoutBuilder<T> with(Consumer<TimeoutBuilder<T>> consumer) {
                consumer.accept(this);
                return this;
            }
        }
    }
}
