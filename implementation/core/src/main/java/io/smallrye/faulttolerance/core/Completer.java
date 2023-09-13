package io.smallrye.faulttolerance.core;

/**
 * Creator and controller of a {@link Future}. An asynchronous computation that
 * produces a future has to create a completer and return its future object.
 * Afterwards, the completer is used to complete the future either with a value,
 * using {@link #complete(Object) Completer.complete()}, or with an error, using
 * {@link #completeWithError(Throwable) Completer.completeWithError()}.
 * <p>
 * If the completer is supplied a cancellation callback using {@link #onCancel(Runnable)},
 * a successful cancellation request on the future calls the cancellation callback.
 *
 * @param <T> type of the result of the computation
 */
public interface Completer<T> {
    /**
     * Creates a new completer.
     *
     * @return a new completer; never {@code null}
     * @param <T> type of the result of the computation
     */
    static <T> Completer<T> create() {
        return new FutureImpl<>();
    }

    /**
     * Completes the future with a value, if pending.
     * If the future is already complete, does nothing.
     *
     * @param value the value with which the future is completed; may be {@code null}
     */
    void complete(T value);

    /**
     * Completes the future with an error, if pending.
     * If the future is already complete, does nothing.
     *
     * @param error the error with which the future is completed; must not be {@code null}
     */
    void completeWithError(Throwable error);

    /**
     * Sets the cancellation callback. Note that this method may be called at most once;
     * subsequent calls will result in an exception.
     *
     * @param cancellationCallback the cancellation callback; may not be {@code null}
     */
    void onCancel(Runnable cancellationCallback);

    /**
     * Returns the future created and controlled by this completer.
     *
     * @return the future; never {@code null}
     */
    Future<T> future();
}
