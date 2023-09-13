package io.smallrye.faulttolerance.core;

import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.VarHandle;
import java.util.concurrent.CancellationException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.function.BiConsumer;

final class FutureImpl<T> implements Future<T>, Completer<T> {
    //
    //  [ PENDING ] ---> [ COMPLETING ] ---> [ COMPLETE ] ---> [ DELIVERED ]
    //               |
    //               +-> [ CANCELLED ]
    //
    private static final int STATE_PENDING = 0;
    private static final int STATE_COMPLETING = 1;
    private static final int STATE_COMPLETE = 2;
    private static final int STATE_DELIVERED = 3;
    private static final int STATE_CANCELLED = 4;

    private static final Lookup LOOKUP = MethodHandles.lookup();
    private static final VarHandle STATE = ConstantBootstraps.fieldVarHandle(LOOKUP,
            "state", VarHandle.class, FutureImpl.class, int.class);
    private static final VarHandle COMPLETION_CALLBACK = ConstantBootstraps.fieldVarHandle(LOOKUP,
            "completionCallback", VarHandle.class, FutureImpl.class, BiConsumer.class);
    private static final VarHandle CANCELLATION_CALLBACK = ConstantBootstraps.fieldVarHandle(LOOKUP,
            "cancellationCallback", VarHandle.class, FutureImpl.class, Runnable.class);

    private static final class ExceptionResult {
        private final Throwable exception;

        private ExceptionResult(Throwable exception) {
            this.exception = exception;
        }
    }

    private volatile int state = STATE_PENDING;
    private volatile Object result; // value or `ExceptionResult` for error

    private volatile BiConsumer<T, Throwable> completionCallback = null;
    private final Barrier completionBarrier = new Barrier();

    private volatile Runnable cancellationCallback = null;

    FutureImpl() {
    }

    @Override
    public void complete(T value) {
        if (STATE.compareAndSet(this, STATE_PENDING, STATE_COMPLETING)) {
            this.result = value;
            this.state = STATE_COMPLETE;
            attemptDelivery();
            completionBarrier.open();
        }
    }

    @Override
    public void completeWithError(Throwable error) {
        checkNotNull(error, "Error must be set");
        if (STATE.compareAndSet(this, STATE_PENDING, STATE_COMPLETING)) {
            this.result = new ExceptionResult(error);
            this.state = STATE_COMPLETE;
            attemptDelivery();
            completionBarrier.open();
        }
    }

    @Override
    public void onCancel(Runnable cancellationCallback) {
        checkNotNull(cancellationCallback, "Cancellation callback must be set");
        if (!CANCELLATION_CALLBACK.compareAndSet(this, null, cancellationCallback)) {
            throw new IllegalStateException("Cancellation callback has already been set");
        }
    }

    @Override
    public Future<T> future() {
        return this;
    }

    @Override
    public void then(BiConsumer<T, Throwable> callback) {
        checkNotNull(callback, "Completion callback must be set");
        if (COMPLETION_CALLBACK.compareAndSet(this, null, callback)) {
            attemptDelivery();
        } else {
            throw new IllegalStateException("Completion callback has already been set");
        }
    }

    @Override
    public void thenComplete(Completer<T> completer) {
        checkNotNull(completer, "Completer must be set");
        then((value, error) -> {
            if (error == null) {
                completer.complete(value);
            } else {
                completer.completeWithError(error);
            }
        });
    }

    @Override
    public boolean isComplete() {
        int state = this.state;
        return state == STATE_COMPLETE || state == STATE_DELIVERED;
    }

    @Override
    public boolean isCancelled() {
        return this.state == STATE_CANCELLED;
    }

    @Override
    public T awaitBlocking() throws Throwable {
        try {
            completionBarrier.await();
        } catch (InterruptedException e) {
            // will throw `InterruptedException` below, if not complete or cancelled
        }

        int state = this.state;
        if (state == STATE_COMPLETE || state == STATE_DELIVERED) {
            Object result = this.result;
            if (result instanceof ExceptionResult) {
                throw ((ExceptionResult) result).exception;
            }
            return (T) result;
        }
        if (state == STATE_CANCELLED) {
            throw new CancellationException();
        }

        // not complete or cancelled, `completionBarrier.await()` above
        // must have thrown `InterruptedException`
        throw new InterruptedException();
    }

    @Override
    public void cancel() {
        if (STATE.compareAndSet(this, STATE_PENDING, STATE_CANCELLED)) {
            Runnable cancellationCallback = this.cancellationCallback;
            if (cancellationCallback != null) {
                cancellationCallback.run();
            }
        }
    }

    private void attemptDelivery() {
        BiConsumer<T, Throwable> callback = this.completionCallback;
        if (callback != null && STATE.compareAndSet(this, STATE_COMPLETE, STATE_DELIVERED)) {
            Object result = this.result;
            if (result instanceof ExceptionResult) {
                callback.accept(null, ((ExceptionResult) result).exception);
            } else {
                callback.accept((T) result, null);
            }
        }
    }

    // adapted from `AbstractQueuedSynchronizer`
    private static class Barrier extends AbstractQueuedSynchronizer {
        void await() throws InterruptedException {
            acquireSharedInterruptibly(1);
        }

        void open() {
            releaseShared(1);
        }

        // ---
        // implementation details

        protected int tryAcquireShared(int ignored) {
            return getState() != 0 ? 1 : -1;
        }

        protected boolean tryReleaseShared(int ignored) {
            setState(1);
            return true;
        }
    }
}
