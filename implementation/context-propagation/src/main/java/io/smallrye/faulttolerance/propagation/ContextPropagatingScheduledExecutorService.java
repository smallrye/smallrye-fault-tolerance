package io.smallrye.faulttolerance.propagation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.microprofile.context.ThreadContext;

final class ContextPropagatingScheduledExecutorService implements ScheduledExecutorService {
    private final ThreadContext threadContext;
    private final ScheduledExecutorService delegate;

    ContextPropagatingScheduledExecutorService(ThreadContext threadContext, ScheduledExecutorService delegate) {
        this.threadContext = threadContext;
        this.delegate = delegate;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return delegate.schedule(threadContext.contextualRunnable(command), delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return delegate.schedule(threadContext.contextualCallable(callable), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return delegate.scheduleAtFixedRate(threadContext.contextualRunnable(command), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return delegate.scheduleWithFixedDelay(threadContext.contextualRunnable(command), initialDelay, delay, unit);
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
        return delegate.submit(threadContext.contextualCallable(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(threadContext.contextualRunnable(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(threadContext.contextualRunnable(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        List<Callable<T>> contextualTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            contextualTasks.add(threadContext.contextualCallable(task));
        }
        return delegate.invokeAll(contextualTasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        List<Callable<T>> contextualTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            contextualTasks.add(threadContext.contextualCallable(task));
        }
        return delegate.invokeAll(contextualTasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        List<Callable<T>> contextualTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            contextualTasks.add(threadContext.contextualCallable(task));
        }
        return delegate.invokeAny(contextualTasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        List<Callable<T>> contextualTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            contextualTasks.add(threadContext.contextualCallable(task));
        }
        return delegate.invokeAny(contextualTasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(threadContext.contextualRunnable(command));
    }
}
