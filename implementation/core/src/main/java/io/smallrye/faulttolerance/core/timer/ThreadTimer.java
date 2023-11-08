package io.smallrye.faulttolerance.core.timer;

import static io.smallrye.faulttolerance.core.timer.TimerLogger.LOG;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import io.smallrye.faulttolerance.core.util.RunnableWrapper;

/**
 * Starts one thread that processes submitted tasks in a loop and when it's time for a task to run,
 * it gets submitted to the executor. The default executor is provided by a caller, so the caller
 * must shut down this timer <em>before</em> shutting down the executor.
 * <p>
 * At most one timer may exist.
 */
public final class ThreadTimer implements Timer {
    private static final Comparator<Task> TASK_COMPARATOR = (o1, o2) -> {
        // two different instances are never equal
        if (o1 == o2) {
            return 0;
        }

        // must _not_ return 0 if start times are equal, because that isn't consistent with `equals` (see also above)
        // must _not_ compare `startTime` using `<` because of how `System.nanoTime()` works (see also below)
        long delta = o1.startTime - o2.startTime;
        if (delta < 0) {
            return -1;
        } else if (delta > 0) {
            return 1;
        }

        return System.identityHashCode(o1) < System.identityHashCode(o2) ? -1 : 1;
    };

    private static volatile ThreadTimer INSTANCE;

    private final SortedSet<Task> tasks = new ConcurrentSkipListSet<>(TASK_COMPARATOR);

    private final Executor defaultExecutor;

    private final Thread thread;

    private final AtomicBoolean running = new AtomicBoolean(true);

    /**
     * Creates a timer with given {@code defaultExecutor}, unless a timer already exists,
     * in which case an exception is thrown.
     *
     * @param defaultExecutor default {@link Executor} used for running scheduled tasks, unless an executor
     *        is provided when {@linkplain #schedule(long, Runnable, Executor) scheduling} a task
     */
    public static synchronized ThreadTimer create(Executor defaultExecutor) {
        ThreadTimer instance = INSTANCE;
        if (instance == null) {
            instance = new ThreadTimer(defaultExecutor);
            INSTANCE = instance;
            return instance;
        }
        throw new IllegalStateException("Timer already exists");
    }

    private ThreadTimer(Executor defaultExecutor) {
        this.defaultExecutor = checkNotNull(defaultExecutor, "Executor must be set");

        this.thread = new Thread(() -> {
            while (running.get()) {
                try {
                    if (tasks.isEmpty()) {
                        LockSupport.park();
                    } else {
                        Task task;
                        try {
                            task = tasks.first();
                        } catch (NoSuchElementException e) {
                            // can happen if all tasks are cancelled right between `tasks.isEmpty` and `tasks.first`
                            continue;
                        }

                        long currentTime = System.nanoTime();
                        long taskStartTime = task.startTime;

                        // must _not_ use `taskStartTime <= currentTime`, because `System.nanoTime()`
                        // is relative to an arbitrary number and so it can possibly overflow;
                        // in such case, `taskStartTime` can be positive, `currentTime` can be negative,
                        //  and yet `taskStartTime` is _before_ `currentTime`
                        if (taskStartTime - currentTime <= 0) {
                            boolean removed = tasks.remove(task);
                            if (removed) {
                                Executor executorForTask = task.executor();
                                if (executorForTask == null) {
                                    executorForTask = defaultExecutor;
                                }

                                executorForTask.execute(task);
                            }
                        } else {
                            // this is OK even if another timer is scheduled during the sleep (even if that timer should
                            // fire sooner than `taskStartTime`), because `schedule` always calls `LockSupport.unpark`
                            LockSupport.parkNanos(taskStartTime - currentTime);
                        }
                    }
                } catch (Throwable e) {
                    // can happen e.g. when the executor is shut down sooner than the timer
                    LOG.unexpectedExceptionInTimerLoop(e);
                }
            }
        }, "SmallRye Fault Tolerance Timer");
        thread.start();

        LOG.createdTimer();
    }

    @Override
    public TimerTask schedule(long delayInMillis, Runnable task) {
        return schedule(delayInMillis, task, null);
    }

    @Override
    public TimerTask schedule(long delayInMillis, Runnable task, Executor executor) {
        long startTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(delayInMillis);
        task = RunnableWrapper.INSTANCE.wrap(task);
        Task timerTask = executor == null || executor == defaultExecutor
                ? new Task(startTime, task)
                : new TaskWithExecutor(startTime, task, executor);
        tasks.add(timerTask);
        LockSupport.unpark(thread);
        LOG.scheduledTimerTask(timerTask, delayInMillis);
        return timerTask;
    }

    @Override
    public int countScheduledTasks() {
        return tasks.size();
    }

    @Override
    public void shutdown() throws InterruptedException {
        if (running.compareAndSet(true, false)) {
            try {
                LOG.shutdownTimer();
                thread.interrupt();
                thread.join();
            } finally {
                INSTANCE = null;
            }
        }
    }

    private static class Task implements TimerTask, Runnable {
        // scheduled: present in the `tasks` queue
        // running: not present in the `tasks` queue && `runnable != null`
        // finished or cancelled: not present in the `tasks` queue && `runnable == null`

        final long startTime; // in nanos, to be compared with System.nanoTime()
        volatile Runnable runnable;

        Task(long startTime, Runnable runnable) {
            this.startTime = startTime;
            this.runnable = checkNotNull(runnable, "Runnable task must be set");
        }

        @Override
        public boolean isDone() {
            ThreadTimer timer = INSTANCE;
            if (timer != null) {
                boolean queued = timer.tasks.contains(this);
                if (queued) {
                    return false;
                } else {
                    return runnable == null;
                }
            }
            return true; // ?
        }

        @Override
        public boolean cancel() {
            ThreadTimer timer = INSTANCE;
            if (timer != null) {
                // can't cancel if it's already running
                boolean removed = timer.tasks.remove(this);
                if (removed) {
                    runnable = null;
                    LOG.cancelledTimerTask(this);
                    return true;
                }
            }
            return false;
        }

        public Executor executor() {
            return null; // default executor of the timer should be used
        }

        @Override
        public void run() {
            LOG.runningTimerTask(this);
            try {
                runnable.run();
            } finally {
                runnable = null;
            }
        }
    }

    private static final class TaskWithExecutor extends Task {
        private final Executor executor;

        TaskWithExecutor(long startTime, Runnable runnable, Executor executor) {
            super(startTime, runnable);
            this.executor = checkNotNull(executor, "Executor must be set");
        }

        @Override
        public Executor executor() {
            return executor;
        }
    }
}
