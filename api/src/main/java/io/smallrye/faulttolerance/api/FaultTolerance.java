package io.smallrye.faulttolerance.api;

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.enterprise.util.TypeLiteral;

import io.smallrye.common.annotation.Experimental;

/**
 * @deprecated use {@link Guard} or {@link TypedGuard}
 */
@Deprecated(forRemoval = true)
@Experimental("first attempt at providing programmatic API")
public interface FaultTolerance<T> {
    /**
     * @deprecated use {@link CircuitBreakerMaintenance#get()}
     */
    @Deprecated(forRemoval = true)
    static CircuitBreakerMaintenance circuitBreakerMaintenance() {
        return SpiAccess.get().circuitBreakerMaintenance();
    }

    /**
     * @deprecated use {@link Guard} or {@link TypedGuard} and {@code adaptCallable}
     */
    @Deprecated(forRemoval = true)
    static <T> Builder<T, Callable<T>> createCallable(Callable<T> action) {
        return SpiAccess.get().newBuilder(ft -> ft.adaptCallable(action));
    }

    /**
     * @deprecated use {@link Guard} or {@link TypedGuard} and {@code adaptSupplier}
     */
    @Deprecated(forRemoval = true)
    static <T> Builder<T, Supplier<T>> createSupplier(Supplier<T> action) {
        return SpiAccess.get().newBuilder(ft -> ft.adaptSupplier(action));
    }

    /**
     * @deprecated use {@link Guard} or {@link TypedGuard}; there's no direct support
     *             for guarding {@code Runnable}s, but adapting to {@code Supplier<Void>} by using
     *             {@code adaptSupplier} should be close enough
     */
    @Deprecated(forRemoval = true)
    static Builder<Void, Runnable> createRunnable(Runnable action) {
        return SpiAccess.get().newBuilder(ft -> ft.adaptRunnable(action));
    }

    /**
     * @deprecated use {@link Guard#create()} or {@link TypedGuard#create(Class)}
     *             or {@link TypedGuard#create(TypeLiteral)}
     */
    @Deprecated(forRemoval = true)
    static <T> Builder<T, FaultTolerance<T>> create() {
        return SpiAccess.get().newBuilder(Function.identity());
    }

    /**
     * @deprecated use {@link Guard} or {@link TypedGuard} and {@code adaptCallable}
     */
    @Deprecated(forRemoval = true)
    static <T> Builder<CompletionStage<T>, Callable<CompletionStage<T>>> createAsyncCallable(
            Callable<CompletionStage<T>> action) {
        return SpiAccess.get().newAsyncBuilder(CompletionStage.class, ft -> ft.adaptCallable(action));
    }

    /**
     * @deprecated use {@link Guard} or {@link TypedGuard} and {@code adaptSupplier}
     */
    @Deprecated(forRemoval = true)
    static <T> Builder<CompletionStage<T>, Supplier<CompletionStage<T>>> createAsyncSupplier(
            Supplier<CompletionStage<T>> action) {
        return SpiAccess.get().newAsyncBuilder(CompletionStage.class, ft -> ft.adaptSupplier(action));
    }

    /**
     * @deprecated use {@link Guard} or {@link TypedGuard}; there's no direct support
     *             for guarding {@code Runnable}s, but adapting to {@code Supplier<Void>} by using
     *             {@code adaptSupplier} should be close enough
     */
    @Deprecated(forRemoval = true)
    static Builder<CompletionStage<Void>, Runnable> createAsyncRunnable(Runnable action) {
        return SpiAccess.get().newAsyncBuilder(CompletionStage.class, ft -> ft.adaptRunnable(action));
    }

    /**
     * @deprecated use {@link Guard#create()} or {@link TypedGuard#create(Class)}
     *             or {@link TypedGuard#create(TypeLiteral)}
     */
    @Deprecated(forRemoval = true)
    static <T> Builder<CompletionStage<T>, FaultTolerance<CompletionStage<T>>> createAsync() {
        return SpiAccess.get().newAsyncBuilder(CompletionStage.class, Function.identity());
    }

    /**
     * @deprecated use {@link Guard#call(Callable, Class)} or {@link Guard#call(Callable, TypeLiteral)}
     *             or {@link TypedGuard#call(Callable)}
     */
    @Deprecated(forRemoval = true)
    T call(Callable<T> action) throws Exception;

    /**
     * @deprecated use {@link Guard#get(Supplier, Class)} or {@link Guard#get(Supplier, TypeLiteral)}
     *             or {@link TypedGuard#get(Supplier)}
     */
    @Deprecated(forRemoval = true)
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
     * @deprecated use {@link Guard#get(Supplier, Class)} or {@link Guard#get(Supplier, TypeLiteral)}
     *             or {@link TypedGuard#get(Supplier)}
     */
    @Deprecated(forRemoval = true)
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
     * @deprecated use {@link Guard#adaptCallable(Callable, Class)} or {@link Guard#adaptCallable(Callable, TypeLiteral)}
     *             or {@link TypedGuard#adaptCallable(Callable)}
     */
    @Deprecated(forRemoval = true)
    default Callable<T> adaptCallable(Callable<T> action) {
        return () -> call(action);
    }

    /**
     * @deprecated use {@link Guard#adaptSupplier(Supplier, Class)} or {@link Guard#adaptSupplier(Supplier, TypeLiteral)}
     *             or {@link TypedGuard#adaptSupplier(Supplier)}
     */
    @Deprecated(forRemoval = true)
    default Supplier<T> adaptSupplier(Supplier<T> action) {
        return () -> get(action);
    }

    /**
     * @deprecated use {@link Guard#adaptSupplier(Supplier, Class)} or {@link Guard#adaptSupplier(Supplier, TypeLiteral)}
     *             or {@link TypedGuard#adaptSupplier(Supplier)}
     */
    @Deprecated(forRemoval = true)
    default Runnable adaptRunnable(Runnable action) {
        return () -> run(action);
    }

    /**
     * @deprecated use {@link Guard} or {@link TypedGuard}, which unifies synchronous and asynchronous invocations
     */
    @Deprecated(forRemoval = true)
    <U> FaultTolerance<U> cast();

    /**
     * @deprecated use {@link Guard} or {@link TypedGuard}, which unifies synchronous and asynchronous invocations
     */
    @Deprecated(forRemoval = true)
    <U> FaultTolerance<U> castAsync(Class<?> asyncType);

    /**
     * @deprecated use {@link Guard.Builder} or {@link TypedGuard.Builder}
     */
    @Deprecated(forRemoval = true)
    interface Builder<T, R> {
        @Deprecated(forRemoval = true)
        Builder<T, R> withDescription(String value);

        @Deprecated(forRemoval = true)
        BulkheadBuilder<T, R> withBulkhead();

        @Deprecated(forRemoval = true)
        CircuitBreakerBuilder<T, R> withCircuitBreaker();

        @Deprecated(forRemoval = true)
        FallbackBuilder<T, R> withFallback();

        @Deprecated(forRemoval = true)
        RateLimitBuilder<T, R> withRateLimit();

        @Deprecated(forRemoval = true)
        RetryBuilder<T, R> withRetry();

        @Deprecated(forRemoval = true)
        TimeoutBuilder<T, R> withTimeout();

        @Deprecated(forRemoval = true)
        Builder<T, R> withThreadOffload(boolean value);

        @Deprecated(forRemoval = true)
        Builder<T, R> withThreadOffloadExecutor(Executor executor);

        @Deprecated(forRemoval = true)
        R build();

        @Deprecated(forRemoval = true)
        default Builder<T, R> with(Consumer<Builder<T, R>> consumer) {
            consumer.accept(this);
            return this;
        }

        @Deprecated(forRemoval = true)
        interface BulkheadBuilder<T, R> {
            @Deprecated(forRemoval = true)
            BulkheadBuilder<T, R> limit(int value);

            @Deprecated(forRemoval = true)
            BulkheadBuilder<T, R> queueSize(int value);

            /**
             * @deprecated use {@code enableSynchronousQueueing()}
             */
            @Deprecated(forRemoval = true)
            BulkheadBuilder<T, R> enableVirtualThreadsQueueing();

            @Deprecated(forRemoval = true)
            BulkheadBuilder<T, R> onAccepted(Runnable callback);

            @Deprecated(forRemoval = true)
            BulkheadBuilder<T, R> onRejected(Runnable callback);

            @Deprecated(forRemoval = true)
            BulkheadBuilder<T, R> onFinished(Runnable callback);

            @Deprecated(forRemoval = true)
            Builder<T, R> done();

            @Deprecated(forRemoval = true)
            default BulkheadBuilder<T, R> with(Consumer<BulkheadBuilder<T, R>> consumer) {
                consumer.accept(this);
                return this;
            }
        }

        @Deprecated(forRemoval = true)
        interface CircuitBreakerBuilder<T, R> {
            @Deprecated(forRemoval = true)
            CircuitBreakerBuilder<T, R> failOn(Collection<Class<? extends Throwable>> value);

            @Deprecated(forRemoval = true)
            default CircuitBreakerBuilder<T, R> failOn(Class<? extends Throwable> value) {
                return failOn(Collections.singleton(Objects.requireNonNull(value)));
            }

            @Deprecated(forRemoval = true)
            CircuitBreakerBuilder<T, R> skipOn(Collection<Class<? extends Throwable>> value);

            @Deprecated(forRemoval = true)
            default CircuitBreakerBuilder<T, R> skipOn(Class<? extends Throwable> value) {
                return skipOn(Collections.singleton(Objects.requireNonNull(value)));
            }

            @Deprecated(forRemoval = true)
            CircuitBreakerBuilder<T, R> when(Predicate<Throwable> value);

            @Deprecated(forRemoval = true)
            CircuitBreakerBuilder<T, R> delay(long value, ChronoUnit unit);

            @Deprecated(forRemoval = true)
            CircuitBreakerBuilder<T, R> requestVolumeThreshold(int value);

            @Deprecated(forRemoval = true)
            CircuitBreakerBuilder<T, R> failureRatio(double value);

            @Deprecated(forRemoval = true)
            CircuitBreakerBuilder<T, R> successThreshold(int value);

            @Deprecated(forRemoval = true)
            CircuitBreakerBuilder<T, R> name(String value);

            @Deprecated(forRemoval = true)
            CircuitBreakerBuilder<T, R> onStateChange(Consumer<CircuitBreakerState> callback);

            @Deprecated(forRemoval = true)
            CircuitBreakerBuilder<T, R> onSuccess(Runnable callback);

            @Deprecated(forRemoval = true)
            CircuitBreakerBuilder<T, R> onFailure(Runnable callback);

            @Deprecated(forRemoval = true)
            CircuitBreakerBuilder<T, R> onPrevented(Runnable callback);

            @Deprecated(forRemoval = true)
            Builder<T, R> done();

            @Deprecated(forRemoval = true)
            default CircuitBreakerBuilder<T, R> with(Consumer<CircuitBreakerBuilder<T, R>> consumer) {
                consumer.accept(this);
                return this;
            }
        }

        @Deprecated(forRemoval = true)
        interface FallbackBuilder<T, R> {
            @Deprecated(forRemoval = true)
            FallbackBuilder<T, R> handler(Supplier<T> value);

            @Deprecated(forRemoval = true)
            FallbackBuilder<T, R> handler(Function<Throwable, T> value);

            @Deprecated(forRemoval = true)
            FallbackBuilder<T, R> applyOn(Collection<Class<? extends Throwable>> value);

            @Deprecated(forRemoval = true)
            default FallbackBuilder<T, R> applyOn(Class<? extends Throwable> value) {
                return applyOn(Collections.singleton(Objects.requireNonNull(value)));
            }

            @Deprecated(forRemoval = true)
            FallbackBuilder<T, R> skipOn(Collection<Class<? extends Throwable>> value);

            @Deprecated(forRemoval = true)
            default FallbackBuilder<T, R> skipOn(Class<? extends Throwable> value) {
                return skipOn(Collections.singleton(Objects.requireNonNull(value)));
            }

            @Deprecated(forRemoval = true)
            FallbackBuilder<T, R> when(Predicate<Throwable> value);

            @Deprecated(forRemoval = true)
            Builder<T, R> done();

            @Deprecated(forRemoval = true)
            default FallbackBuilder<T, R> with(Consumer<FallbackBuilder<T, R>> consumer) {
                consumer.accept(this);
                return this;
            }
        }

        @Deprecated(forRemoval = true)
        interface RateLimitBuilder<T, R> {
            @Deprecated(forRemoval = true)
            RateLimitBuilder<T, R> limit(int value);

            @Deprecated(forRemoval = true)
            RateLimitBuilder<T, R> window(long value, ChronoUnit unit);

            @Deprecated(forRemoval = true)
            RateLimitBuilder<T, R> minSpacing(long value, ChronoUnit unit);

            @Deprecated(forRemoval = true)
            RateLimitBuilder<T, R> type(RateLimitType value);

            @Deprecated(forRemoval = true)
            RateLimitBuilder<T, R> onPermitted(Runnable callback);

            @Deprecated(forRemoval = true)
            RateLimitBuilder<T, R> onRejected(Runnable callback);

            @Deprecated(forRemoval = true)
            Builder<T, R> done();

            @Deprecated(forRemoval = true)
            default RateLimitBuilder<T, R> with(Consumer<RateLimitBuilder<T, R>> consumer) {
                consumer.accept(this);
                return this;
            }
        }

        @Deprecated(forRemoval = true)
        interface RetryBuilder<T, R> {
            @Deprecated(forRemoval = true)
            RetryBuilder<T, R> maxRetries(int value);

            @Deprecated(forRemoval = true)
            RetryBuilder<T, R> delay(long value, ChronoUnit unit);

            @Deprecated(forRemoval = true)
            RetryBuilder<T, R> maxDuration(long value, ChronoUnit unit);

            @Deprecated(forRemoval = true)
            RetryBuilder<T, R> jitter(long value, ChronoUnit unit);

            @Deprecated(forRemoval = true)
            RetryBuilder<T, R> retryOn(Collection<Class<? extends Throwable>> value);

            @Deprecated(forRemoval = true)
            default RetryBuilder<T, R> retryOn(Class<? extends Throwable> value) {
                return retryOn(Collections.singleton(Objects.requireNonNull(value)));
            }

            @Deprecated(forRemoval = true)
            RetryBuilder<T, R> abortOn(Collection<Class<? extends Throwable>> value);

            @Deprecated(forRemoval = true)
            default RetryBuilder<T, R> abortOn(Class<? extends Throwable> value) {
                return abortOn(Collections.singleton(Objects.requireNonNull(value)));
            }

            @Deprecated(forRemoval = true)
            RetryBuilder<T, R> whenResult(Predicate<Object> value);

            @Deprecated(forRemoval = true)
            default RetryBuilder<T, R> when(Predicate<Throwable> value) {
                return whenException(value);
            }

            @Deprecated(forRemoval = true)
            RetryBuilder<T, R> whenException(Predicate<Throwable> value);

            @Deprecated(forRemoval = true)
            RetryBuilder<T, R> beforeRetry(Runnable value);

            @Deprecated(forRemoval = true)
            RetryBuilder<T, R> beforeRetry(Consumer<Throwable> value);

            @Deprecated(forRemoval = true)
            ExponentialBackoffBuilder<T, R> withExponentialBackoff();

            @Deprecated(forRemoval = true)
            FibonacciBackoffBuilder<T, R> withFibonacciBackoff();

            @Deprecated(forRemoval = true)
            CustomBackoffBuilder<T, R> withCustomBackoff();

            @Deprecated(forRemoval = true)
            RetryBuilder<T, R> onRetry(Runnable callback);

            @Deprecated(forRemoval = true)
            RetryBuilder<T, R> onSuccess(Runnable callback);

            @Deprecated(forRemoval = true)
            RetryBuilder<T, R> onFailure(Runnable callback);

            @Deprecated(forRemoval = true)
            Builder<T, R> done();

            @Deprecated(forRemoval = true)
            default RetryBuilder<T, R> with(Consumer<RetryBuilder<T, R>> consumer) {
                consumer.accept(this);
                return this;
            }

            @Deprecated(forRemoval = true)
            interface ExponentialBackoffBuilder<T, R> {
                @Deprecated(forRemoval = true)
                ExponentialBackoffBuilder<T, R> factor(int value);

                @Deprecated(forRemoval = true)
                ExponentialBackoffBuilder<T, R> maxDelay(long value, ChronoUnit unit);

                @Deprecated(forRemoval = true)
                RetryBuilder<T, R> done();

                @Deprecated(forRemoval = true)
                default ExponentialBackoffBuilder<T, R> with(Consumer<ExponentialBackoffBuilder<T, R>> consumer) {
                    consumer.accept(this);
                    return this;
                }
            }

            @Deprecated(forRemoval = true)
            interface FibonacciBackoffBuilder<T, R> {
                @Deprecated(forRemoval = true)
                FibonacciBackoffBuilder<T, R> maxDelay(long value, ChronoUnit unit);

                @Deprecated(forRemoval = true)
                RetryBuilder<T, R> done();

                @Deprecated(forRemoval = true)
                default FibonacciBackoffBuilder<T, R> with(Consumer<FibonacciBackoffBuilder<T, R>> consumer) {
                    consumer.accept(this);
                    return this;
                }
            }

            @Deprecated(forRemoval = true)
            interface CustomBackoffBuilder<T, R> {
                @Deprecated(forRemoval = true)
                CustomBackoffBuilder<T, R> strategy(Supplier<CustomBackoffStrategy> value);

                @Deprecated(forRemoval = true)
                RetryBuilder<T, R> done();

                @Deprecated(forRemoval = true)
                default CustomBackoffBuilder<T, R> with(Consumer<CustomBackoffBuilder<T, R>> consumer) {
                    consumer.accept(this);
                    return this;
                }
            }
        }

        @Deprecated(forRemoval = true)
        interface TimeoutBuilder<T, R> {
            @Deprecated(forRemoval = true)
            TimeoutBuilder<T, R> duration(long value, ChronoUnit unit);

            @Deprecated(forRemoval = true)
            TimeoutBuilder<T, R> onTimeout(Runnable callback);

            @Deprecated(forRemoval = true)
            TimeoutBuilder<T, R> onFinished(Runnable callback);

            @Deprecated(forRemoval = true)
            Builder<T, R> done();

            @Deprecated(forRemoval = true)
            default TimeoutBuilder<T, R> with(Consumer<TimeoutBuilder<T, R>> consumer) {
                consumer.accept(this);
                return this;
            }
        }
    }
}
