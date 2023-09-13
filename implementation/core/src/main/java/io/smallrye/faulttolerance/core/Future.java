package io.smallrye.faulttolerance.core;

import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

/**
 * Represents a computation that may still be running, and allows obtaining
 * a result of the computation once available.
 * <p>
 * When the computation is still running, we say that the future is <em>pending</em>.
 * Once the computation finishes, we say that the future is <em>complete</em>.
 * Alternatively, after successful cancellation, we say the future is <em>cancelled</em>.
 * The computation may finish with two kinds of outcomes: success, producing
 * a <em>value</em>, and failure, producing an <em>error</em>.
 * <p>
 * To obtain the result, consumers of the future are expected to register
 * a <em>completion callback</em> using {@link #then(BiConsumer) Future.then()}.
 * The callback is called with a pair [value, error] once the future completes.
 * To distinguish a sucessful outcome from a failure, the error should be
 * tested. If the error is {@code null}, the outcome is successful. Note that
 * a success may still produce a {@code null} value.
 * <p>
 * The callback may be registered before or after the future completes,
 * and is guaranteed to be called exactly once. The thread on which the callback
 * is called is not specified; it may be the thread that completes the future
 * or the thread that registers the callback. Only one callback may be registered;
 * attempts to register a second callback end up with an exception.
 * <p>
 * Future objects are created by a {@link Completer}. The {@link Completer} is
 * also the only way through which the future may be completed. For convenience,
 * static factory methods are provided to construct already complete futures:
 * {@link #of(Object) Future.of()}, {@link #ofError(Throwable) Future.ofError()},
 * and {@link #from(Callable) Future.from()}.
 * <p>
 * A future consumer may request cancellation of the computation by calling
 * {@link #cancel() Future.cancel()}. This is only possible while the future is
 * pending; when the future is already complete, this is a noop.
 * <p>
 * Unlike common {@code Future} abstractions, this one is fairly limited.
 * There may only be one completion callback, and there are no combinators
 * such as {@code map} or {@code flatMap}.
 *
 * @param <T> type of the result of the computation
 */
public interface Future<T> {
    /**
     * Returns a future that is already complete with given {@code value}.
     *
     * @param value the value; may be {@code null}
     * @return the future that is already complete with the value; never {@code null}
     * @param <T> type of the value
     */
    static <T> Future<T> of(T value) {
        Completer<T> completer = Completer.create();
        completer.complete(value);
        return completer.future();
    }

    /**
     * Returns a future that is already complete with given {@code error}.
     *
     * @param error the error; must not be {@code null}
     * @return the future that is already complete with the error; never {@code null}
     * @param <T> type of hypothetical result; only for type inference
     */
    static <T> Future<T> ofError(Throwable error) {
        Completer<T> completer = Completer.create();
        completer.completeWithError(error);
        return completer.future();
    }

    /**
     * Returns a future that is already complete with the outcome of given {@code callable}
     * (which may be a returned value or a thrown error).
     *
     * @param callable the callable to call; must not be {@code null}
     * @return the future that is complete with the outcome of the {@code callable}; never {@code null}
     * @param <T> type of the result of given {@code callable}
     */
    static <T> Future<T> from(Callable<T> callable) {
        Completer<T> completer = Completer.create();
        try {
            T result = callable.call();
            completer.complete(result);
        } catch (Exception e) {
            completer.completeWithError(e);
        }
        return completer.future();
    }

    /**
     * Registers a completion callback with this future. The first argument
     * of the {@link BiConsumer} is the value of the future, the second argument
     * is the error.
     * <p>
     * Value may be {@code null} in case of a success, but error is never {@code null}
     * in case of a failure. Therefore, idiomatic usage looks like:
     *
     * <pre>
     * future.then((value, error) -&gt; {
     *     if (error == null) {
     *         ... use value ...
     *     } else {
     *         ... use error ...
     *     }
     * });
     * </pre>
     *
     * @param callback the completion callback to be registered; must not be {@code null}
     */
    void then(BiConsumer<T, Throwable> callback);

    /**
     * Registers a completion callback with this future. The callback forwards
     * the result of this future into the given completer.
     *
     * @param completer the completer to which the result of this future is forwarded;
     *        must not be {@code null}
     */
    void thenComplete(Completer<T> completer);

    /**
     * Returns whether this future is complete.
     *
     * @return {@code true} if this future is complete, {@code false} otherwise
     */
    boolean isComplete();

    /**
     * Returns whether this future is cancelled.
     *
     * @return {@code true} if this future is cancelled, {@code false} otherwise
     */
    boolean isCancelled();

    /**
     * Blocks the calling thread until this future is complete or cancelled,
     * and then returns the value of this future or throws the error, or throws
     * {@link java.util.concurrent.CancellationException CancellationException}.
     * In case this future is already complete or cancelled when this method
     * is called, no blocking occurs.
     * <p>
     * The blocked thread may be interrupted, in which case this method throws
     * {@link InterruptedException}.
     * <p>
     * This method should rarely be used without previous checking with {@link #isComplete()}
     * or {@link #isCancelled()}.
     *
     * @return the value of this future; may be {@code null}
     * @throws Throwable the error of this future, {@code CancellationException} or {@code InterruptedException}
     */
    T awaitBlocking() throws Throwable;

    /**
     * Requests cancellation of the computation represented by this future.
     *
     * @see Completer
     */
    void cancel();
}
