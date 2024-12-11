package io.smallrye.faulttolerance.core.timeout;

import static io.smallrye.faulttolerance.core.timeout.TimeoutLogger.LOG;
import static java.util.concurrent.CompletableFuture.failedFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.timer.Timer;
import io.smallrye.faulttolerance.core.timer.TimerTask;

public class CompletionStageTimeout<V> extends Timeout<CompletionStage<V>> {
    public CompletionStageTimeout(FaultToleranceStrategy<CompletionStage<V>> delegate, String description, long timeoutInMillis,
            Timer timer) {
        super(delegate, description, timeoutInMillis, timer);
    }

    @Override
    public CompletionStage<V> apply(InvocationContext<CompletionStage<V>> ctx) {
        LOG.trace("CompletionStageTimeout started");
        try {
            return doApply(ctx);
        } finally {
            LOG.trace("CompletionStageTimeout finished");
        }
    }

    private CompletionStage<V> doApply(InvocationContext<CompletionStage<V>> ctx) {
        CompletableFuture<V> result = new CompletableFuture<>();

        ctx.fireEvent(TimeoutEvents.Started.INSTANCE);

        TimeoutExecution execution = new TimeoutExecution(null, () -> {
            LOG.debugf("%s invocation timed out (%d ms)", description, timeoutInMillis);
            ctx.fireEvent(TimeoutEvents.Finished.TIMED_OUT);
            result.completeExceptionally(timeoutException(description));
        });
        TimerTask task = timer.schedule(timeoutInMillis, execution::timeoutAndInterrupt, ctx.get(Executor.class));

        CompletionStage<V> originalResult;
        try {
            originalResult = delegate.apply(ctx);
        } catch (Exception e) {
            originalResult = failedFuture(e);
        }

        originalResult.whenComplete((value, exception) -> {
            // if the execution timed out, this will be a noop
            //
            // this comes first, so that when the future is completed, the timeout watcher is already cancelled
            // (this isn't exactly needed, but makes tests easier to write)
            execution.finish(task::cancel);

            if (execution.hasTimedOut()) {
                // the "on timeout" callback is called by `execution::timeoutAndInterrupt` above
            } else if (exception != null) {
                ctx.fireEvent(TimeoutEvents.Finished.NORMALLY);
                result.completeExceptionally(exception);
            } else {
                ctx.fireEvent(TimeoutEvents.Finished.NORMALLY);
                result.complete(value);
            }
        });

        return result;
    }
}
