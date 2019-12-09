package io.smallrye.faulttolerance.propagation;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
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
    public ExecutorService getGlobalExecutorService(int size, int queueSize) {
        return ManagedExecutor.builder().maxAsync(size).maxQueued(queueSize).build();
    }

    @Override
    public ExecutorService createExecutorService(int size, int queueSize, BlockingQueue<Runnable> taskQueue) {
        SmallRyeManagedExecutor.Builder builder = (SmallRyeManagedExecutor.Builder) ManagedExecutor.builder();
        ExecutorService executorService = new ThreadPoolExecutor(size, queueSize, 0, TimeUnit.MILLISECONDS, taskQueue);
        return builder.maxAsync(size).maxQueued(queueSize).withExecutorService(executorService).build();
    }

    @Override
    public int priority() {
        return 100;
    }
}
