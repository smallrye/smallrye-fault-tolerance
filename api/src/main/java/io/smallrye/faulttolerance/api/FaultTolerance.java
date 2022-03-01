package io.smallrye.faulttolerance.api;

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import io.smallrye.common.annotation.Experimental;

/**
 * Allows guarding an action with various fault tolerance strategies: bulkhead, circuit breaker, fallback, retry, and
 * timeout. Synchronous as well as asynchronous actions may be guarded, asynchronous actions may optionally be
 * offloaded to another thread. The only supported type for asynchronous actions is {@link CompletionStage}.
 * <p>
 * An instance of this interface represents a configured set of fault tolerance strategies. It can be used to
 * guard a {@link #call(Callable) Callable}, {@link #get(Supplier) Supplier} or {@link #run(Runnable) Runnable}
 * invocation, or adapt an unguarded {@link #adaptCallable(Callable) Callable}, {@link #adaptSupplier(Supplier) Supplier}
 * or {@link #adaptRunnable(Runnable) Runnable} to a guarded one.
 * <p>
 * The {@code create*} and {@code createAsync*} methods return a builder that allows configuring all fault tolerance
 * strategies. Order of builder method invocations does not matter, the fault tolerance strategies are always applied
 * in a predefined order: fallback &gt; retry &gt; circuit breaker &gt; timeout &gt; bulkhead &gt; thread offload &gt;
 * guarded action.
 * <p>
 * Two styles of usage are possible. The {@link #createCallable(Callable)} and {@link #createAsyncCallable(Callable)}
 * methods return a builder that, at the end, returns a {@code Callable}. This is convenient in case you only want to
 * guard a single action (which is possibly invoked multiple times). Similar methods exist for the {@code Supplier}
 * and {@code Runnable} types.
 * <p>
 * The {@link #create()} and {@link #createAsync()} methods return a builder that, at the end, returns
 * a {@code FaultTolerance} instance, which is useful when you need to guard multiple actions with the same set
 * of fault tolerance strategies. Note that circuit breakers and bulkheads are stateful, so there's a big difference
 * between guarding multiple actions using the same {@code FaultTolerance} object and using a separate
 * {@code FaultTolerance} object for each action. Using a single {@code FaultTolerance} instance to guard multiple
 * actions means that a single circuit breaker and/or bulkhead will be shared among all those actions.
 * <p>
 * This API is essentially a programmatic equivalent to the declarative, annotation-based API of MicroProfile Fault
 * Tolerance and SmallRye Fault Tolerance. It shares the set of fault tolerance strategies, their invocation order
 * and behavior, their configuration properties, etc. Notable differences are:
 * <ul>
 * <li>asynchronous actions of type {@link Future} are not supported;</li>
 * <li>the fallback, circuit breaker and retry strategies always inspect the cause chain of exceptions,
 * following the behavior of SmallRye Fault Tolerance in the non-compatible mode.</li>
 * </ul>
 *
 * @param <T> type of value of the guarded action
 */
@Experimental("first attempt at providing programmatic API")
public interface FaultTolerance<T> {
    /**
     * Returns a {@link CircuitBreakerMaintenance} instance that provides maintenance access to existing
     * circuit breakers.
     */
    static CircuitBreakerMaintenance circuitBreakerMaintenance() {
        return FaultToleranceSpiAccess.get().circuitBreakerMaintenance();
    }

    /**
     * Returns a builder that, at the end, returns a {@link Callable} guarding the given {@code action}.
     * The {@code action} is synchronous and is always executed on the original thread.
     */
    static <T> Builder<T, Callable<T>> createCallable(Callable<T> action) {
        return FaultToleranceSpiAccess.get().newBuilder(ft -> ft.adaptCallable(action));
    }

    /**
     * Returns a builder that, at the end, returns a {@link Supplier} guarding the given {@code action}.
     * The {@code action} is synchronous and is always executed on the original thread.
     */
    static <T> Builder<T, Supplier<T>> createSupplier(Supplier<T> action) {
        return FaultToleranceSpiAccess.get().newBuilder(ft -> ft.adaptSupplier(action));
    }

    /**
     * Returns a builder that, at the end, returns a {@link Runnable} guarding the given {@code action}.
     * The {@code action} is synchronous and is always executed on the original thread.
     */
    static Builder<Void, Runnable> createRunnable(Runnable action) {
        return FaultToleranceSpiAccess.get().newBuilder(ft -> ft.adaptRunnable(action));
    }

    /**
     * Returns a builder that, at the end, returns a {@link FaultTolerance} object representing a set of configured
     * fault tolerance strategies. It can be used to execute synchronous actions using {@link #call(Callable)},
     * {@link #get(Supplier)} or {@link #run(Runnable)}.
     * <p>
     * This method usually has to be called with an explicitly provided type argument. For example:
     * {@code FaultTolerance.&lt;String>create()}.
     */
    static <T> Builder<T, FaultTolerance<T>> create() {
        return FaultToleranceSpiAccess.get().newBuilder(Function.identity());
    }

    /**
     * Returns a builder that, at the end, returns a {@link Callable} guarding the given {@code action}.
     * The {@code action} is asynchronous and may be offloaded to another thread.
     */
    static <T> Builder<CompletionStage<T>, Callable<CompletionStage<T>>> createAsyncCallable(
            Callable<CompletionStage<T>> action) {
        return FaultToleranceSpiAccess.get().newAsyncBuilder(CompletionStage.class, ft -> ft.adaptCallable(action));
    }

    /**
     * Returns a builder that, at the end, returns a {@link Supplier} guarding the given {@code action}.
     * The {@code action} is asynchronous and may be offloaded to another thread.
     */
    static <T> Builder<CompletionStage<T>, Supplier<CompletionStage<T>>> createAsyncSupplier(
            Supplier<CompletionStage<T>> action) {
        return FaultToleranceSpiAccess.get().newAsyncBuilder(CompletionStage.class, ft -> ft.adaptSupplier(action));
    }

    /**
     * Returns a builder that, at the end, returns a {@link Runnable} guarding the given {@code action}.
     * The {@code action} is asynchronous and may be offloaded to another thread.
     */
    static Builder<CompletionStage<Void>, Runnable> createAsyncRunnable(Runnable action) {
        return FaultToleranceSpiAccess.get().newAsyncBuilder(CompletionStage.class, ft -> ft.adaptRunnable(action));
    }

    /**
     * Returns a builder that, at the end, returns a {@link FaultTolerance} object representing a set of configured
     * fault tolerance strategies. It can be used to execute asynchronous actions using {@link #call(Callable)},
     * {@link #get(Supplier)} or {@link #run(Runnable)}.
     * <p>
     * This method usually has to be called with an explicitly provided type argument. For example:
     * {@code FaultTolerance.&lt;String>createAsync()}.
     */
    static <T> Builder<CompletionStage<T>, FaultTolerance<CompletionStage<T>>> createAsync() {
        return FaultToleranceSpiAccess.get().newAsyncBuilder(CompletionStage.class, Function.identity());
    }

    /**
     * Calls given {@code action} and guards the call by this configured set of fault tolerance strategies.
     * If this {@code FaultTolerance} instance was created using {@link #create()}, the action is synchronous
     * and is always executed on the same thread that calls this method. If this {@code FaultTolerance} instance
     * was created using {@link #createAsync()}, the action is asynchronous and may be offloaded to another thread
     * depending on how the builder was configured.
     */
    T call(Callable<T> action) throws Exception;

    /**
     * Calls given {@code action} and guards the call by this configured set of fault tolerance strategies.
     * If this {@code FaultTolerance} instance was created using {@code create*}, the action is synchronous
     * and is always executed on the same thread that calls this method. If this {@code FaultTolerance} instance
     * was created using {@code createAsync*}, the action is asynchronous and may be offloaded to another thread
     * depending on how the builder was configured.
     */
    default T get(Supplier<T> action) {
        try {
            return call(action::get);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Calls given {@code action} and guards the call by this configured set of fault tolerance strategies.
     * If this {@code FaultTolerance} instance was created using {@link #create()}, the action is synchronous
     * and is always executed on the same thread that calls this method. If this {@code FaultTolerance} instance
     * was created using {@link #createAsync()}, the action is asynchronous and may be offloaded to another thread
     * depending on how the builder was configured.
     */
    default void run(Runnable action) {
        try {
            call(() -> {
                action.run();
                return null;
            });
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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
     * Adapts given {@code action} to an action guarded by this configured set of fault tolerance strategies.
     * Useful when the action has to be called multiple times.
     * <p>
     * Equivalent to {@code () -> run(action)}.
     *
     * @see #run(Runnable)
     */
    default Runnable adaptRunnable(Runnable action) {
        return () -> run(action);
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
     * @param <R> type of result of this builder, depends on how this builder was created
     */
    interface Builder<T, R> {
        /**
         * Assigns a description to the resulting set of configured fault tolerance strategies. The description
         * is used in logging messages and exception messages.
         * <p>
         * The description may be an arbitrary string. Duplicates are permitted.
         * <p>
         * If no description is set, a random UUID is used.
         *
         * @param value a description, must not be {@code null}
         * @return this fault tolerance builder
         */
        Builder<T, R> withDescription(String value);

        /**
         * Adds a bulkhead strategy. In this API, bulkhead is a simple concurrency limiter.
         *
         * @return a builder to configure the bulkhead strategy
         * @see Bulkhead @Bulkhead
         */
        BulkheadBuilder<T, R> withBulkhead();

        /**
         * Adds a circuit breaker strategy.
         *
         * @return a builder to configure the circuit breaker strategy
         * @see CircuitBreaker @CircuitBreaker
         */
        CircuitBreakerBuilder<T, R> withCircuitBreaker();

        /**
         * Adds a fallback strategy.
         *
         * @return a builder to configure the fallback strategy
         * @see Fallback @Fallback
         */
        FallbackBuilder<T, R> withFallback();

        /**
         * Adds a retry strategy. Retry uses constant backoff between attempts by default,
         * but may be configured to use exponential backoff, Fibonacci backoff, or custom backoff.
         *
         * @return a builder to configure the retry strategy
         * @see Retry @Retry
         */
        RetryBuilder<T, R> withRetry();

        /**
         * Adds a timeout strategy.
         *
         * @return a builder to configure the timeout strategy
         * @see Timeout @Timeout
         */
        TimeoutBuilder<T, R> withTimeout();

        /**
         * Configures whether the guarded action should be offloaded to another thread. This is only possible
         * for asynchronous actions. If this builder was not created using {@code createAsync}, this method
         * throws an exception.
         *
         * @param value whether the guarded action should be offloaded to another thread
         * @return this fault tolerance builder
         * @see Asynchronous @Asynchronous
         */
        Builder<T, R> withThreadOffload(boolean value);

        /**
         * Returns a ready-to-use instance of {@code FaultTolerance} or guarded {@code Callable}, depending on
         * how this builder was created.
         */
        R build();

        /**
         * Syntactic sugar for calling the builder methods conditionally without breaking the invocation chain.
         * For example:
         *
         * <pre>
         * {@code
         * FaultTolerance.create(this::doSomething)
         *     .withFallback() ... .done()
         *     .with(builder -> {
         *         if (useTimeout) {
         *             builder.withTimeout() ... .done();
         *         }
         *     })
         *     .build();
         * }
         * </pre>
         *
         * @param consumer block of code to execute with this builder
         * @return this fault tolerance builder
         */
        default Builder<T, R> with(Consumer<Builder<T, R>> consumer) {
            consumer.accept(this);
            return this;
        }

        /**
         * Configures a bulkhead.
         *
         * @see Bulkhead @Bulkhead
         */
        interface BulkheadBuilder<T, R> {
            /**
             * Sets the concurrency limit the bulkhead will enforce. Defaults to 10.
             *
             * @param value the concurrency limit, must be &gt;= 1
             * @return this bulkhead builder
             */
            BulkheadBuilder<T, R> limit(int value);

            /**
             * Sets the maximum size of the bulkhead queue. Defaults to 10.
             * <p>
             * May only be called if the builder configures fault tolerance for asynchronous actions. In other words,
             * if the builder was not created using {@code createAsync}, this method throws an exception.
             *
             * @param value the queue size, must be &gt;= 1
             * @return this bulkhead builder
             */
            BulkheadBuilder<T, R> queueSize(int value);

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
            BulkheadBuilder<T, R> onAccepted(Runnable callback);

            /**
             * Sets a callback that will be invoked when this bulkhead rejects an invocation.
             * <p>
             * The callback must be fast and non-blocking and must not throw an exception.
             *
             * @param callback the rejected callback, must not be {@code null}
             * @return this bulkhead builder
             */
            BulkheadBuilder<T, R> onRejected(Runnable callback);

            /**
             * Sets a callback that will be invoked when a finished invocation leaves this bulkhead.
             * <p>
             * The callback must be fast and non-blocking and must not throw an exception.
             *
             * @param callback the finished callback, must not be {@code null}
             * @return this bulkhead builder
             */
            BulkheadBuilder<T, R> onFinished(Runnable callback);

            /**
             * Returns the original fault tolerance builder.
             *
             * @return the original fault tolerance builder
             */
            Builder<T, R> done();

            default BulkheadBuilder<T, R> with(Consumer<BulkheadBuilder<T, R>> consumer) {
                consumer.accept(this);
                return this;
            }
        }

        /**
         * Configures a circuit breaker.
         *
         * @see CircuitBreaker @CircuitBreaker
         */
        interface CircuitBreakerBuilder<T, R> {
            /**
             * Sets the set of exception types considered failure. Defaults to all exceptions ({@code Throwable}).
             *
             * @param value collection of exception types, must not be {@code null}
             * @return this circuit breaker builder
             * @see CircuitBreaker#failOn() @CircuitBreaker.failOn
             */
            CircuitBreakerBuilder<T, R> failOn(Collection<Class<? extends Throwable>> value);

            /**
             * Equivalent to {@link #failOn(Collection) failOn(Collections.singleton(value))}.
             *
             * @param value an exception class, must not be {@code null}
             * @return this circuit breaker builder
             */
            default CircuitBreakerBuilder<T, R> failOn(Class<? extends Throwable> value) {
                return failOn(Collections.singleton(Objects.requireNonNull(value)));
            }

            /**
             * Sets the set of exception types considered success. Defaults to no exception (empty set).
             *
             * @param value collection of exception types, must not be {@code null}
             * @return this circuit breaker builder
             * @see CircuitBreaker#skipOn() @CircuitBreaker.skipOn
             */
            CircuitBreakerBuilder<T, R> skipOn(Collection<Class<? extends Throwable>> value);

            /**
             * Equivalent to {@link #skipOn(Collection) skipOn(Collections.singleton(value))}.
             *
             * @param value an exception class, must not be {@code null}
             * @return this circuit breaker builder
             */
            default CircuitBreakerBuilder<T, R> skipOn(Class<? extends Throwable> value) {
                return skipOn(Collections.singleton(Objects.requireNonNull(value)));
            }

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
            CircuitBreakerBuilder<T, R> when(Predicate<Throwable> value);

            /**
             * Sets the delay after which an open circuit moves to half-open. Defaults to 5 seconds.
             *
             * @param value the delay length, must be &gt;= 0
             * @param unit the delay unit, must not be {@code null}
             * @return this circuit breaker builder
             * @see CircuitBreaker#delay() @CircuitBreaker.delay
             * @see CircuitBreaker#delayUnit() @CircuitBreaker.delayUnit
             */
            CircuitBreakerBuilder<T, R> delay(long value, ChronoUnit unit);

            /**
             * Sets the size of the circuit breaker's rolling window.
             *
             * @param value the size of the circuit breaker's rolling window, must be &gt;= 1
             * @return this circuit breaker builder
             * @see CircuitBreaker#requestVolumeThreshold() @CircuitBreaker.requestVolumeThreshold
             */
            CircuitBreakerBuilder<T, R> requestVolumeThreshold(int value);

            /**
             * Sets the failure ratio that, once reached, will move a closed circuit breaker to open. Defaults to 0.5.
             *
             * @param value the failure ratio, must be &gt;= 0 and &lt;= 1
             * @return this circuit breaker builder
             * @see CircuitBreaker#failureRatio() @CircuitBreaker.failureRatio
             */
            CircuitBreakerBuilder<T, R> failureRatio(double value);

            /**
             * Sets the number of successful executions that, once reached, will move a half-open circuit breaker
             * to closed. Defaults to 1.
             *
             * @param value the number of successful executions, must be &gt;= 1
             * @return this circuit breaker builder
             * @see CircuitBreaker#successThreshold() @CircuitBreaker.successThreshold
             */
            CircuitBreakerBuilder<T, R> successThreshold(int value);

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
            CircuitBreakerBuilder<T, R> name(String value);

            /**
             * Sets a callback that will be invoked upon each state change of this circuit breaker.
             * <p>
             * The callback must be fast and non-blocking and must not throw an exception.
             *
             * @param callback the state change callback, must not be {@code null}
             * @return this circuit breaker builder
             */
            CircuitBreakerBuilder<T, R> onStateChange(Consumer<CircuitBreakerState> callback);

            /**
             * Sets a callback that will be invoked when this circuit breaker treats a finished invocation as success.
             * <p>
             * The callback must be fast and non-blocking and must not throw an exception.
             *
             * @param callback the success callback, must not be {@code null}
             * @return this circuit breaker builder
             */
            CircuitBreakerBuilder<T, R> onSuccess(Runnable callback);

            /**
             * Sets a callback that will be invoked when this circuit breaker treats a finished invocation as failure.
             * <p>
             * The callback must be fast and non-blocking and must not throw an exception.
             *
             * @param callback the failure callback, must not be {@code null}
             * @return this circuit breaker builder
             */
            CircuitBreakerBuilder<T, R> onFailure(Runnable callback);

            /**
             * Sets a callback that will be invoked when this circuit breaker prevents an invocation, because it is
             * in the open or half-open state.
             * <p>
             * The callback must be fast and non-blocking and must not throw an exception.
             *
             * @param callback the prevented callback, must not be {@code null}
             * @return this circuit breaker builder
             */
            CircuitBreakerBuilder<T, R> onPrevented(Runnable callback);

            /**
             * Returns the original fault tolerance builder.
             *
             * @return the original fault tolerance builder
             */
            Builder<T, R> done();

            default CircuitBreakerBuilder<T, R> with(Consumer<CircuitBreakerBuilder<T, R>> consumer) {
                consumer.accept(this);
                return this;
            }
        }

        /**
         * Configures a fallback.
         *
         * @see Fallback @Fallback
         */
        interface FallbackBuilder<T, R> {
            /**
             * Sets the fallback handler in the form of a fallback value {@link Supplier}.
             *
             * @param value the fallback value supplier, must not be {@code null}
             * @return this fallback builder
             */
            FallbackBuilder<T, R> handler(Supplier<T> value);

            /**
             * Sets the fallback handler in the form of a {@link Function} that transforms the exception
             * to the fallback value.
             *
             * @param value the fallback value function, must not be {@code null}
             * @return this fallback builder
             */
            FallbackBuilder<T, R> handler(Function<Throwable, T> value);

            /**
             * Sets the set of exception types considered failure. Defaults to all exceptions ({@code Throwable}).
             *
             * @param value collection of exception types, must not be {@code null}
             * @return this fallback builder
             * @see Fallback#applyOn() @Fallback.applyOn
             */
            FallbackBuilder<T, R> applyOn(Collection<Class<? extends Throwable>> value);

            /**
             * Equivalent to {@link #applyOn(Collection) applyOn(Collections.singleton(value))}.
             *
             * @param value an exception class, must not be {@code null}
             * @return this fallback builder
             */
            default FallbackBuilder<T, R> applyOn(Class<? extends Throwable> value) {
                return applyOn(Collections.singleton(Objects.requireNonNull(value)));
            }

            /**
             * Sets the set of exception types considered success. Defaults to no exception (empty set).
             *
             * @param value collection of exception types, must not be {@code null}
             * @return this fallback builder
             * @see Fallback#skipOn() @Fallback.skipOn
             */
            FallbackBuilder<T, R> skipOn(Collection<Class<? extends Throwable>> value);

            /**
             * Equivalent to {@link #skipOn(Collection) skipOn(Collections.singleton(value))}.
             *
             * @param value an exception class, must not be {@code null}
             * @return this fallback builder
             */
            default FallbackBuilder<T, R> skipOn(Class<? extends Throwable> value) {
                return skipOn(Collections.singleton(Objects.requireNonNull(value)));
            }

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
            FallbackBuilder<T, R> when(Predicate<Throwable> value);

            /**
             * Returns the original fault tolerance builder.
             *
             * @return the original fault tolerance builder
             */
            Builder<T, R> done();

            default FallbackBuilder<T, R> with(Consumer<FallbackBuilder<T, R>> consumer) {
                consumer.accept(this);
                return this;
            }
        }

        /**
         * Configures a retry.
         *
         * @see Retry @Retry
         */
        interface RetryBuilder<T, R> {
            /**
             * Sets the maximum number of retries. Defaults to 3.
             *
             * @param value the maximum number of retries, must be &gt;= -1.
             * @return this retry builder
             * @see Retry#maxRetries() @Retry.maxRetries
             */
            RetryBuilder<T, R> maxRetries(int value);

            /**
             * Sets the delay between retries. Defaults to 0.
             *
             * @param value the delay length, must be &gt;= 0
             * @param unit the delay unit, must not be {@code null}
             * @return this retry builder
             * @see Retry#delay() @Retry.delay
             * @see Retry#delayUnit() @Retry.delayUnit
             */
            RetryBuilder<T, R> delay(long value, ChronoUnit unit);

            /**
             * Sets the maximum duration of all invocations, including possible retries. Defaults to 3 minutes.
             *
             * @param value the maximum duration length, must be &gt;= 0
             * @param unit the maximum duration unit, must not be {@code null}
             * @return this retry builder
             * @see Retry#maxDuration() @Retry.maxDuration
             * @see Retry#durationUnit() @Retry.durationUnit
             */
            RetryBuilder<T, R> maxDuration(long value, ChronoUnit unit);

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
            RetryBuilder<T, R> jitter(long value, ChronoUnit unit);

            /**
             * Sets the set of exception types considered failure. Defaults to all exceptions ({@code Exception}).
             *
             * @param value collection of exception types, must not be {@code null}
             * @return this retry builder
             * @see Retry#retryOn() @Retry.retryOn
             */
            RetryBuilder<T, R> retryOn(Collection<Class<? extends Throwable>> value);

            /**
             * Equivalent to {@link #retryOn(Collection) retryOn(Collections.singleton(value))}.
             *
             * @param value an exception class, must not be {@code null}
             * @return this retry builder
             */
            default RetryBuilder<T, R> retryOn(Class<? extends Throwable> value) {
                return retryOn(Collections.singleton(Objects.requireNonNull(value)));
            }

            /**
             * Sets the set of exception types considered success. Defaults to no exception (empty set).
             *
             * @param value collection of exception types, must not be {@code null}
             * @return this retry builder
             * @see Retry#abortOn() @Retry.abortOn
             */
            RetryBuilder<T, R> abortOn(Collection<Class<? extends Throwable>> value);

            /**
             * Equivalent to {@link #abortOn(Collection) abortOn(Collections.singleton(value))}.
             *
             * @param value an exception class, must not be {@code null}
             * @return this retry builder
             */
            default RetryBuilder<T, R> abortOn(Class<? extends Throwable> value) {
                return abortOn(Collections.singleton(Objects.requireNonNull(value)));
            }

            /**
             * Sets a predicate to determine when an exception should be considered failure
             * and retry should be attempted. This is a more general variant of {@link #retryOn(Collection) retryOn}.
             * Note that there is no generalized {@link #abortOn(Collection) abortOn}, because all exceptions
             * that do not match this predicate are implicitly considered success.
             * <p>
             * If this method is called, {@code retryOn} and {@code abortOn} may not be called.
             *
             * @param value the predicate, must not be {@code null}
             * @return this fallback builder
             */
            RetryBuilder<T, R> when(Predicate<Throwable> value);

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
            ExponentialBackoffBuilder<T, R> withExponentialBackoff();

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
            FibonacciBackoffBuilder<T, R> withFibonacciBackoff();

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
            CustomBackoffBuilder<T, R> withCustomBackoff();

            /**
             * Sets a callback that will be invoked when a retry is attempted.
             * <p>
             * The callback must be fast and non-blocking and must not throw an exception.
             *
             * @param callback the retried callback, must not be {@code null}
             * @return this retry builder
             */
            RetryBuilder<T, R> onRetry(Runnable callback);

            /**
             * Sets a callback that will be invoked when this retry strategy treats a finished invocation as success,
             * regardless of whether a retry was attempted or not.
             * <p>
             * The callback must be fast and non-blocking and must not throw an exception.
             *
             * @param callback the retried callback, must not be {@code null}
             * @return this retry builder
             */
            RetryBuilder<T, R> onSuccess(Runnable callback);

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
            RetryBuilder<T, R> onFailure(Runnable callback);

            /**
             * Returns the original fault tolerance builder.
             *
             * @return the original fault tolerance builder
             */
            Builder<T, R> done();

            default RetryBuilder<T, R> with(Consumer<RetryBuilder<T, R>> consumer) {
                consumer.accept(this);
                return this;
            }

            /**
             * Configures an exponential backoff for retry.
             *
             * @see ExponentialBackoff @ExponentialBackoff
             */
            interface ExponentialBackoffBuilder<T, R> {
                /**
                 * Sets the multiplicative factor used to determine delay between retries. Defaults to 2.
                 *
                 * @param value the multiplicative factor, must be &gt;= 1
                 * @return this exponential backoff builder
                 * @see ExponentialBackoff#factor() @ExponentialBackoff.factor
                 */
                ExponentialBackoffBuilder<T, R> factor(int value);

                /**
                 * Sets the maximum delay between retries. Defaults to 1 minute.
                 *
                 * @param value the maximum delay, must be &gt;= 0
                 * @param unit the maximum delay unit, must not be {@code null}
                 * @return this exponential backoff builder
                 * @see ExponentialBackoff#maxDelay() @ExponentialBackoff.maxDelay
                 * @see ExponentialBackoff#maxDelayUnit() @ExponentialBackoff.maxDelayUnit
                 */
                ExponentialBackoffBuilder<T, R> maxDelay(long value, ChronoUnit unit);

                /**
                 * Returns the original retry builder.
                 *
                 * @return the original retry builder
                 */
                RetryBuilder<T, R> done();

                default ExponentialBackoffBuilder<T, R> with(Consumer<ExponentialBackoffBuilder<T, R>> consumer) {
                    consumer.accept(this);
                    return this;
                }
            }

            /**
             * Configures a Fibonacci backoff for retry.
             *
             * @see FibonacciBackoff @FibonacciBackoff
             */
            interface FibonacciBackoffBuilder<T, R> {
                /**
                 * Sets the maximum delay between retries. Defaults to 1 minute.
                 *
                 * @param value the maximum delay, must be &gt;= 0
                 * @param unit the maximum delay unit, must not be {@code null}
                 * @return this fibonacci backoff builder
                 * @see FibonacciBackoff#maxDelay() @FibonacciBackoff.maxDelay
                 * @see FibonacciBackoff#maxDelayUnit() @FibonacciBackoff.maxDelayUnit
                 */
                FibonacciBackoffBuilder<T, R> maxDelay(long value, ChronoUnit unit);

                /**
                 * Returns the original retry builder.
                 *
                 * @return the original retry builder
                 */
                RetryBuilder<T, R> done();

                default FibonacciBackoffBuilder<T, R> with(Consumer<FibonacciBackoffBuilder<T, R>> consumer) {
                    consumer.accept(this);
                    return this;
                }
            }

            /**
             * Configures a custom backoff for retry.
             *
             * @see CustomBackoff @CustomBackoff
             */
            interface CustomBackoffBuilder<T, R> {
                /**
                 * Sets the custom backoff strategy in the form of a {@link Supplier} of {@link CustomBackoffStrategy}
                 * instances. Mandatory.
                 *
                 * @see CustomBackoff#value()
                 */
                CustomBackoffBuilder<T, R> strategy(Supplier<CustomBackoffStrategy> value);

                /**
                 * Returns the original retry builder.
                 */
                RetryBuilder<T, R> done();

                default CustomBackoffBuilder<T, R> with(Consumer<CustomBackoffBuilder<T, R>> consumer) {
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
        interface TimeoutBuilder<T, R> {
            /**
             * Sets the timeout duration. Defaults to 1 second.
             *
             * @param value the timeout length, must be &gt;= 0
             * @param unit the timeout unit, must not be {@code null}
             * @return this timeout builder
             * @see Timeout#value() @Timeout.value
             * @see Timeout#unit() @Timeout.unit
             */
            TimeoutBuilder<T, R> duration(long value, ChronoUnit unit);

            /**
             * Sets a callback that will be invoked when an invocation times out.
             * <p>
             * The callback must be fast and non-blocking and must not throw an exception.
             *
             * @param callback the timeout callback, must not be {@code null}
             * @return this timeout builder
             */
            TimeoutBuilder<T, R> onTimeout(Runnable callback);

            /**
             * Sets a callback that will be invoked when an invocation finishes before the timeout.
             * <p>
             * The callback must be fast and non-blocking and must not throw an exception.
             *
             * @param callback the finished callback, must not be {@code null}
             * @return this timeout builder
             */
            TimeoutBuilder<T, R> onFinished(Runnable callback);

            /**
             * Returns the original fault tolerance builder.
             *
             * @return the original fault tolerance builder
             */
            Builder<T, R> done();

            default TimeoutBuilder<T, R> with(Consumer<TimeoutBuilder<T, R>> consumer) {
                consumer.accept(this);
                return this;
            }
        }
    }
}
