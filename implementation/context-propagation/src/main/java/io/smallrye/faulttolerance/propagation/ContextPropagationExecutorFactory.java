package io.smallrye.faulttolerance.propagation;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.context.ManagedExecutor;

import io.smallrye.context.SmallRyeManagedExecutor;
import io.smallrye.faulttolerance.ExecutorFactory;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class ContextPropagationExecutorFactory implements ExecutorFactory {

    @Override
    public ExecutorService createExecutorService(int size, int queueSize) {
        if (queueSize == 0) {
            return createExecutorService(size, queueSize, new SynchronousQueue<>());
        } else {
            return ManagedExecutor.builder().maxAsync(size).maxQueued(queueSize).build();
        }
    }

    @Override
    public ExecutorService createExecutorService(int size, int queueSize, BlockingQueue<Runnable> taskQueue) {
        // only to work around MP CP limit:
        queueSize = Math.max(1, queueSize);

        SmallRyeManagedExecutor.Builder builder = (SmallRyeManagedExecutor.Builder) ManagedExecutor.builder();
        ExecutorService executorService = new ThreadPoolExecutor(1, size, 10 * 60, TimeUnit.SECONDS, taskQueue);
        return builder.maxAsync(size).maxQueued(queueSize).withExecutorService(executorService).build();
    }

    @Override
    public ScheduledExecutorService createTimeoutExecutor(int size) {
        return Executors.newScheduledThreadPool(size);
    }

    @Override
    public int priority() {
        return 100;
    }
}
