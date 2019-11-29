package com.github.ladicek.oaken_ocean.core.timeout;

import com.github.ladicek.oaken_ocean.core.Cancellator;
import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.FutureOrFailure;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import static com.github.ladicek.oaken_ocean.core.util.Preconditions.check;
import static com.github.ladicek.oaken_ocean.core.util.Preconditions.checkNotNull;

public class Timeout<V> implements FaultToleranceStrategy<V> {
    final FaultToleranceStrategy<V> delegate;
    final String description;

    final long timeoutInMillis;
    final TimeoutWatcher watcher;
    final MetricsRecorder metricsRecorder;
    final Executor asyncExecutor;

    public Timeout(FaultToleranceStrategy<V> delegate, String description, long timeoutInMillis, TimeoutWatcher watcher,
                   MetricsRecorder metricsRecorder, Executor asyncExecutor) {
        this.delegate = checkNotNull(delegate, "Timeout delegate must be set");
        this.description = checkNotNull(description, "Timeout description must be set");
        this.timeoutInMillis = check(timeoutInMillis, timeoutInMillis > 0, "Timeout must be > 0");
        this.watcher = checkNotNull(watcher, "Timeout watcher must be set");
        this.metricsRecorder = metricsRecorder == null ? MetricsRecorder.NO_OP : metricsRecorder;
        this.asyncExecutor = asyncExecutor;
    }

    @Override
    public V asyncFutureApply(Callable<V> target, Cancellator cancellator) throws Exception {
        if (asyncExecutor == null) {
            throw new IllegalStateException("Async Future execution requires asyncExecutor");
        }
        FutureOrFailure<?> result = new FutureOrFailure<>();
        asyncExecutor.execute(
              () -> {
                  TimeoutExecution execution =
                        new TimeoutExecution(Thread.currentThread(), result::timeout, timeoutInMillis);
                  TimeoutWatch watch = watcher.schedule(execution);
                  long start = System.nanoTime();

                  Exception exception = null;
                  boolean interrupted = false;
                  try {
                      result.setDelegate((Future)delegate.asyncFutureApply(target, cancellator));
                  } catch (InterruptedException e) {
                      interrupted = true;
                  } catch (Exception e) {
                      exception = e;
                  } finally {
                      // if the execution already timed out, this will be a noop
                      execution.finish(watch::cancel);
                  }

                  if (Thread.interrupted()) {
                      // using `Thread.interrupted()` intentionally and unconditionally, because per MP FT spec, chapter 6.1,
                      // interruption status must be cleared when the method returns
                      interrupted = true;
                  }

                  if (interrupted && !execution.hasTimedOut()) {
                      exception = new InterruptedException();
                  }

                  long end = System.nanoTime();
                  if (execution.hasTimedOut()) {
                      metricsRecorder.timeoutTimedOut(end - start);
                      result.setFailure(timeoutException());
                  } else if (exception != null) {
                      result.setFailure(exception);
                      metricsRecorder.timeoutFailed(end - start);
                  } else {
                      metricsRecorder.timeoutSucceeded(end - start);
                  }
              }
        );
        result.waitForFutureInitialization();
        return (V)result;
    }

    @Override
    public V apply(Callable<V> target) throws Exception {
        TimeoutExecution execution = new TimeoutExecution(Thread.currentThread(), null, timeoutInMillis);
        TimeoutWatch watch = watcher.schedule(execution);
        long start = System.nanoTime();

        V result = null;
        Exception exception = null;
        boolean interrupted = false;
        try {
            result = delegate.apply(target);
        } catch (InterruptedException e) {
            interrupted = true;
        } catch (Exception e) {
            exception = e;
        } finally {
            // if the execution already timed out, this will be a noop
            execution.finish(watch::cancel);
        }

        if (Thread.interrupted()) {
            // using `Thread.interrupted()` intentionally and unconditionally, because per MP FT spec, chapter 6.1,
            // interruption status must be cleared when the method returns
            interrupted = true;
        }

        if (interrupted && !execution.hasTimedOut()) {
            exception = new InterruptedException();
        }

        long end = System.nanoTime();
        if (execution.hasTimedOut()) {
            metricsRecorder.timeoutTimedOut(end - start);
            throw timeoutException();
        }

        if (exception != null) {
            metricsRecorder.timeoutFailed(end - start);
            throw exception;
        }

        metricsRecorder.timeoutSucceeded(end - start);

        return result;
    }

    TimeoutException timeoutException() {
        return new TimeoutException(description + " timed out");
    }

    public interface MetricsRecorder {
        void timeoutSucceeded(long time);
        void timeoutTimedOut(long time);
        void timeoutFailed(long time);

        MetricsRecorder NO_OP = new MetricsRecorder() {
            @Override
            public void timeoutSucceeded(long time) {
            }

            @Override
            public void timeoutTimedOut(long time) {
            }

            @Override
            public void timeoutFailed(long time) {
            }
        };
    }
}
