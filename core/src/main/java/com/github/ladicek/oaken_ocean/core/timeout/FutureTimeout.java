package com.github.ladicek.oaken_ocean.core.timeout;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.FutureInvocationContext;
import com.github.ladicek.oaken_ocean.core.FutureOrFailure;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class FutureTimeout<V> extends TimeoutBase<Future<V>, FutureInvocationContext<V>> {

    private final Executor asyncExecutor;

    public FutureTimeout(FaultToleranceStrategy<Future<V>, FutureInvocationContext<V>> delegate, String description, long timeoutInMillis, TimeoutWatcher watcher, MetricsRecorder metricsRecorder, Executor asyncExecutor) {
        super(delegate, description, timeoutInMillis, watcher, metricsRecorder);
        this.asyncExecutor = asyncExecutor;
    }


    @Override
    public Future<V> apply(FutureInvocationContext<V> context) throws Exception {
        if (asyncExecutor == null) {
            throw new IllegalStateException("Async Future execution requires asyncExecutor");
        }
        FutureOrFailure<V> result = new FutureOrFailure<>();
        asyncExecutor.execute(
              () -> {
                  TimeoutExecution execution =
                        new TimeoutExecution(Thread.currentThread(), result::timeout, timeoutInMillis);
                  TimeoutWatch watch = watcher.schedule(execution);
                  long start = System.nanoTime();

                  Exception exception = null;
                  boolean interrupted = false;
                  try {
                      result.setDelegate(delegate.apply(context));
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
        return result;
    }
}
