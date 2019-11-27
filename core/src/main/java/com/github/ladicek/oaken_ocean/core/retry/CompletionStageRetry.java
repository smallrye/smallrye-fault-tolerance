package com.github.ladicek.oaken_ocean.core.retry;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.stopwatch.RunningStopwatch;
import com.github.ladicek.oaken_ocean.core.stopwatch.Stopwatch;
import com.github.ladicek.oaken_ocean.core.util.SetOfThrowables;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import static com.github.ladicek.oaken_ocean.core.util.Preconditions.checkNotNull;

public class CompletionStageRetry<V> extends Retry<CompletionStage<V>> {
    public CompletionStageRetry(FaultToleranceStrategy<CompletionStage<V>> delegate,
                                String description,
                                SetOfThrowables retryOn,
                                SetOfThrowables abortOn,
                                long maxRetries, long maxTotalDurationInMillis,
                                Delay delayBetweenRetries, Stopwatch stopwatch,
                                Retry.MetricsRecorder metricsRecorder) {
        super(delegate, description, retryOn, abortOn, maxRetries, maxTotalDurationInMillis, delayBetweenRetries, stopwatch, metricsRecorder);
    }
    // mstodo reattempt should not be triggered before the previous one. So we should be good
    // mstodo move to the call class if we were to separate from the context class

    @Override
    public CompletionStage<V> apply(Callable<CompletionStage<V>> target) throws Exception {
        RunningStopwatch runningStopwatch = stopwatch.start();

        return doRetry(target, 0, runningStopwatch, null);
    }

    public CompletionStage<V> doRetry(Callable<CompletionStage<V>> target,
                                      int attempt,
                                      RunningStopwatch stopwatch,
                                      Throwable latestFailure)
          throws Exception {
        if (attempt > 0 && attempt <= maxRetries) {
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
        }
        if (stopwatch.elapsedTimeInMillis() > maxTotalDurationInMillis) {
            if (latestFailure != null) {
                metricsRecorder.retryFailed(); // mstodo a single place to bump it
                throw getError(latestFailure);
            } else {
                metricsRecorder.retryFailed();
                throw new FaultToleranceException(description + " reached max retries or max retry duration");
            }
        }
        try {
            return delegate.apply(target)
                  .thenApply()
                  .handleAsync((value, error) -> {
                     if (value != null) {
                         recordSuccess(attempt);
                         return value;
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
            doRetry(target, attempt+1, stopwatch, th);
        }
    }

    private Exception getError(Throwable latestFailure) throws Exception {
        // TODO: TCK expects that but it seems un(der)specified in the spec
        if (latestFailure instanceof Exception) {
            return (Exception)latestFailure; // mstodo is it okay?
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


//        Throwable latestFailure = null;
//        while (counter <= maxRetries && runningStopwatch.elapsedTimeInMillis() < maxTotalDurationInMillis) {
//            if (counter > 0) {
//                metricsRecorder.retryRetried();
//            }
//            try {
//                V result = delegate.call();
//                if (counter == 0) {
//                    metricsRecorder.retrySucceededNotRetried();
//                } else {
//                    metricsRecorder.retrySucceededRetried();
//                }
//                return result;
//            } catch (InterruptedException e) {
//                throw e;
//            } catch (Throwable e) {
//                if (Thread.interrupted()) {
//                    metricsRecorder.retryFailed();
//                    throw new InterruptedException();
//                }
//
//                // specifying `abortOn` is only useful when it's more specific than `retryOn`;
//                // otherwise, if the exception isn't present in `retryOn`, it's always an abort
//                if (abortOn.includes(e.getClass()) || !retryOn.includes(e.getClass())) {
//                    metricsRecorder.retryFailed();
//                    throw e;
//                }
//                latestFailure = e;
//            }
//
//            try {
//                delayBetweenRetries.sleep();
//            } catch (InterruptedException e) {
//                metricsRecorder.retryFailed();
//                throw e;
//            } catch (Exception e) {
//                metricsRecorder.retryFailed();
//                if (Thread.interrupted()) {
//                    throw new InterruptedException();
//                }
//
//                throw e;
//            }
//
//            counter++;
//        }
//
//        if (latestFailure != null) {
//            metricsRecorder.retryFailed(); // mstodo a single place to bump it
//
//            // TODO: TCK expects that but it seems un(der)specified in the spec
//            if (latestFailure instanceof Exception) {
//                throw (Exception)latestFailure; // mstodo is it okay?
//            } else {
//                throw new FaultToleranceException(latestFailure.getMessage(), latestFailure);
//            }
//        } else {
//            metricsRecorder.retryFailed();
//            throw new FaultToleranceException(description + " reached max retries or max retry duration");
//        }
//    }

    public void cancel() {
        executionThread.interrupt(); // mstodo handle if needed
    }

    public interface MetricsRecorder {
        void retrySucceededNotRetried();

        void retrySucceededRetried();

        void retryFailed();

        void retryRetried();

        MetricsRecorder NO_OP = new MetricsRecorder() {
            @Override
            public void retrySucceededNotRetried() {
            }

            @Override
            public void retrySucceededRetried() {
            }

            @Override
            public void retryFailed() {
            }

            @Override
            public void retryRetried() {
            }
        };
    }
}