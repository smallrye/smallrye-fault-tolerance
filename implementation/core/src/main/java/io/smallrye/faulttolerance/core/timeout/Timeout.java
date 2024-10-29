
package io.smallrye.faulttolerance.core.timeout;

import static io.smallrye.faulttolerance.core.timeout.TimeoutLogger.LOG;
import static io.smallrye.faulttolerance.core.util.Preconditions.check;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

import io.smallrye.faulttolerance.core.Completer;
import io.smallrye.faulttolerance.core.FaultToleranceContext;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.timer.Timer;
import io.smallrye.faulttolerance.core.timer.TimerTask;

public class Timeout<V> implements FaultToleranceStrategy<V> {
    private final FaultToleranceStrategy<V> delegate;
    private final String description;

    private final long timeoutInMillis;
    private final Timer timer;

    public Timeout(FaultToleranceStrategy<V> delegate, String description, long timeoutInMillis, Timer timer) {
        this.delegate = checkNotNull(delegate, "Timeout delegate must be set");
        this.description = checkNotNull(description, "Timeout description must be set");
        this.timeoutInMillis = check(timeoutInMillis, timeoutInMillis > 0, "Timeout must be > 0");
        this.timer = checkNotNull(timer, "Timer must be set");
    }

    @Override
    public Future<V> apply(FaultToleranceContext<V> ctx) {
        LOG.trace("Timeout started");
        try {
            Completer<V> result = Completer.create();

            ctx.fireEvent(TimeoutEvents.Started.INSTANCE);

            // must extract `FutureTimeoutNotification` early, because if retries are present,
            // a different `FutureTimeoutNotification` may be present in the `InvocationContext`
            // by the time the timeout callback is invoked
            FutureTimeoutNotification notification = ctx.remove(FutureTimeoutNotification.class);

            AtomicBoolean completedWithTimeout = new AtomicBoolean(false);
            Runnable onTimeout = () -> {
                if (completedWithTimeout.compareAndSet(false, true)) {
                    LOG.debugf("%s invocation timed out (%d ms)", description, timeoutInMillis);
                    ctx.fireEvent(TimeoutEvents.Finished.TIMED_OUT);
                    TimeoutException timeout = new TimeoutException(description + " timed out");
                    if (notification != null) {
                        notification.accept(timeout);
                    }
                    result.completeWithError(timeout);
                }
            };

            Thread executingThread = ctx.isSync() ? Thread.currentThread() : null;
            TimeoutExecution execution = new TimeoutExecution(executingThread, timeoutInMillis, onTimeout);
            TimerTask task = timer.schedule(execution.timeoutInMillis(), execution::timeoutAndInterrupt);

            Future<V> originalResult;
            try {
                originalResult = delegate.apply(ctx);
            } catch (Exception e) {
                originalResult = Future.ofError(e);
            }

            originalResult.then((value, error) -> {
                // if the execution timed out, this will be a noop
                //
                // this comes first, so that when the future is completed, the timeout watcher is already cancelled
                // (this isn't exactly needed, but makes tests easier to write)
                execution.finish(task::cancel);

                if (ctx.isSync() && Thread.interrupted()) {
                    // using `Thread.interrupted()` intentionally, because per MP FT spec, chapter 6.1,
                    // interruption status must be cleared when the method returns
                    error = new InterruptedException();
                }

                if (execution.hasTimedOut()) {
                    onTimeout.run();
                } else if (error == null) {
                    ctx.fireEvent(TimeoutEvents.Finished.NORMALLY);
                    result.complete(value);
                } else {
                    ctx.fireEvent(TimeoutEvents.Finished.NORMALLY);
                    result.completeWithError(error);
                }
            });

            return result.future();
        } finally {
            LOG.trace("Timeout finished");
        }
    }

}
