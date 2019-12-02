package com.github.ladicek.oaken_ocean.core.retry;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.SimpleInvocationContext;
import com.github.ladicek.oaken_ocean.core.stopwatch.RunningStopwatch;
import com.github.ladicek.oaken_ocean.core.stopwatch.Stopwatch;
import com.github.ladicek.oaken_ocean.core.util.SetOfThrowables;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

public class CompletionStageRetry<V> extends SyncRetry<CompletionStage<V>> {
    public CompletionStageRetry(FaultToleranceStrategy<CompletionStage<V>, SimpleInvocationContext<CompletionStage<V>>> delegate,
                                String description,
                                SetOfThrowables retryOn,
                                SetOfThrowables abortOn,
                                long maxRetries, long maxTotalDurationInMillis,
                                Delay delayBetweenRetries, Stopwatch stopwatch,
                                SyncRetry.MetricsRecorder metricsRecorder) {
        super(delegate, description, retryOn, abortOn, maxRetries, maxTotalDurationInMillis, delayBetweenRetries, stopwatch, metricsRecorder);
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
                throw e;
            } catch (Exception e) {
                metricsRecorder.retryFailed();
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                throw e;
            }
        } else {
            metricsRecorder.retryFailed();
            throw getError(latestFailure);
        }
        if (stopwatch.elapsedTimeInMillis() > maxTotalDurationInMillis) {
            if (latestFailure != null) {
                metricsRecorder.retryFailed();
                throw getError(latestFailure);
            } else {
                metricsRecorder.retryFailed();
                throw new FaultToleranceException(description + " reached max retries or max retry duration");
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
                          if (abortOn.includes(error.getClass()) || !retryOn.includes(error.getClass())) {
                              metricsRecorder.retryFailed();
                              throw new CompletionException(error);
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
            return doRetry(target, attempt + 1, stopwatch, th);
        }
    }

    private Exception getError(Throwable latestFailure) throws Exception {
        // TODO: TCK expects that but it seems un(der)specified in the spec
        if (latestFailure instanceof Exception) {
            return (Exception) latestFailure;
        } else {
            return new FaultToleranceException(latestFailure.getMessage(), latestFailure);
        }
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