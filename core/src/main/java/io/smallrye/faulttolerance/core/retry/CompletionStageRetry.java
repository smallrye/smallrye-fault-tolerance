package io.smallrye.faulttolerance.core.retry;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.SimpleInvocationContext;
import io.smallrye.faulttolerance.core.stopwatch.RunningStopwatch;
import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;

public class CompletionStageRetry<V> extends RetryBase<CompletionStage<V>, SimpleInvocationContext<CompletionStage<V>>> {
    public CompletionStageRetry(
            FaultToleranceStrategy<CompletionStage<V>, SimpleInvocationContext<CompletionStage<V>>> delegate,
            String description,
            SetOfThrowables retryOn,
            SetOfThrowables abortOn,
            long maxRetries, long maxTotalDurationInMillis,
            Delay delayBetweenRetries, Stopwatch stopwatch,
            MetricsRecorder metricsRecorder) {
        super(delegate, description, retryOn, abortOn, maxRetries, maxTotalDurationInMillis, delayBetweenRetries, stopwatch,
                metricsRecorder);
    }

    @Override
    public CompletionStage<V> apply(SimpleInvocationContext<CompletionStage<V>> context) throws Exception {
        RunningStopwatch runningStopwatch = stopwatch.start();
        return doRetry(context, 0, runningStopwatch, null);
    }

    public CompletionStage<V> doRetry(SimpleInvocationContext<CompletionStage<V>> target,
            int attempt,
            RunningStopwatch stopwatch,
            Throwable latestFailure)
            throws Exception {
        if (attempt == 0) {
            // do not sleep
        } else if (attempt <= maxRetries) {
            metricsRecorder.retryRetried();
            try {
                delayBetweenRetries.sleep();
            } catch (InterruptedException e) {
                metricsRecorder.retryFailed();
                return erroneousResult(e);
            } catch (Exception e) {
                metricsRecorder.retryFailed();
                if (Thread.interrupted()) {
                    return erroneousResult(new InterruptedException());
                }

                return erroneousResult(e);
            }
        } else {
            metricsRecorder.retryFailed();
            return erroneousResult(latestFailure);
        }
        if (stopwatch.elapsedTimeInMillis() > maxTotalDurationInMillis) {
            if (latestFailure != null) {
                metricsRecorder.retryFailed();
                return erroneousResult(latestFailure);
            } else {
                metricsRecorder.retryFailed();
                return erroneousResult(new FaultToleranceException(description + " reached max retries or max retry duration"));
            }
        }
        try {
            return delegate.apply(target)
                    .handle(DelegateResultCarrier::new)
                    .thenCompose(result -> {
                        Throwable error = result.error;
                        if (error == null) {
                            recordSuccess(attempt);
                            return CompletableFuture.completedFuture(result.value);
                        } else {
                            metricsRecorder.retryFailed();
                            if (shouldAbortRetrying(error)) {
                                metricsRecorder.retryFailed();
                                if (error instanceof RuntimeException) {
                                    throw (RuntimeException) error;
                                } else {
                                    throw new CompletionException(error);
                                }
                            } else {
                                try {
                                    return doRetry(target, attempt + 1, stopwatch, error);
                                } catch (CompletionException ce) {
                                    throw ce;
                                } catch (Exception any) {
                                    throw new CompletionException(any);
                                }
                            }
                        }
                    });
        } catch (Throwable th) {
            if (shouldAbortRetrying(th)) {
                return erroneousResult(th);
            } else {
                return doRetry(target, attempt + 1, stopwatch, th);
            }
        }
    }

    private CompletionStage<V> erroneousResult(Throwable latestFailure) {
        CompletableFuture<V> result = new CompletableFuture<>();
        // TODO: TCK expects that but it seems un(der)specified in the spec
        Exception error;
        if (latestFailure instanceof Exception) {
            error = (Exception) latestFailure;
        } else {
            error = new FaultToleranceException(latestFailure.getMessage(), latestFailure);
        }
        result.completeExceptionally(error);
        return result;
    }

    private void recordSuccess(int attempt) {
        if (attempt == 0) {
            metricsRecorder.retrySucceededNotRetried();
        } else {
            metricsRecorder.retrySucceededRetried();
        }
    }

    private class DelegateResultCarrier {
        final V value;
        final Throwable error;

        private DelegateResultCarrier(V value, Throwable error) {
            this.value = value;
            this.error = error;
        }
    }
}
