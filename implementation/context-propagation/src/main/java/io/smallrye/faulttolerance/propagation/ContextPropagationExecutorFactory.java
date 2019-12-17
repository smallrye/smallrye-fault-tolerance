package io.smallrye.faulttolerance.propagation;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

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
        // mstodo remove
        ExecutorService executorService = new MyExeService(new ThreadPoolExecutor(2, size, 10 * 60, TimeUnit.SECONDS, taskQueue,
                new PropagatingFactory()));
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

    private static class PropagatingFactory implements ThreadFactory {
        private static final AtomicInteger poolIds = new AtomicInteger(0);
        private final AtomicInteger threadId = new AtomicInteger(0);
        private final int poolId = poolIds.getAndIncrement();

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(() -> {
                r.run();
                try {
                    if (threadId.get() == 1) {
                        Thread.sleep(300L);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace(); // mstodo remove
                }
            });
            thread.setName("context-propagating-pool-" + poolId + "-thread-" + threadId.getAndIncrement());
            return thread;
        }
    }

    private class MyExeService implements ExecutorService {
        ThreadPoolExecutor delegate;

        public MyExeService(ThreadPoolExecutor threadPoolExecutor) {
            this.delegate = threadPoolExecutor;
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return delegate.submit(task);
        }

        private void slp(long time) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return delegate.submit(task, result);
        }

        @Override
        public Future<?> submit(Runnable task) {
            return delegate.submit(task);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return delegate.invokeAll(tasks);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException {
            return delegate.invokeAll(tasks, timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return delegate.invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.invokeAny(tasks, timeout, unit);
        }

        @Override
        public void execute(Runnable command) {
//            delegate.execute(() -> slp(300));
            delegate.execute(() -> {
                command.run();
            });
        }
    }
}
