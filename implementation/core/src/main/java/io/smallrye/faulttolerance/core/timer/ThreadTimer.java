package io.smallrye.faulttolerance.core.timer;

import static io.smallrye.faulttolerance.core.timer.TimerLogger.LOG;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

import io.smallrye.faulttolerance.core.util.RunnableWrapper;

/**
 * Starts one thread that processes submitted tasks in a loop and when it's time for a task to run,
 * it gets submitted to the executor. The default executor is provided by a caller, so the caller
 * must shut down this timer <em>before</em> shutting down the executor.
 */
// TODO implement a hashed wheel?
public final class ThreadTimer implements Timer {
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private static final Comparator<Task> TASK_COMPARATOR = (o1, o2) -> {
        if (o1 == o2) {
            // two different instances are never equal
            return 0;
        }

        // must _not_ return 0 if start times are equal, because that isn't consistent
        // with `equals` (see also above)
        return o1.startTime <= o2.startTime ? -1 : 1;
    };

    private final String name;

    private final SortedSet<Task> tasks;

    private final Thread thread;

    private final AtomicBoolean running = new AtomicBoolean(true);

    /**
     * @param defaultExecutor default {@link Executor} used for running scheduled tasks, unless an executor
     *        is provided when {@linkplain #schedule(long, Runnable, Executor) scheduling} a task
     */
    public ThreadTimer(Executor defaultExecutor) {
        checkNotNull(defaultExecutor, "Executor must be set");

        this.name = "SmallRye Fault Tolerance Timer " + COUNTER.incrementAndGet();
        LOG.createdTimer(name);

        this.tasks = new ConcurrentSkipListSet<>(TASK_COMPARATOR);
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
                            tasks.remove(task);
                            if (STATE.compareAndSet(task, Task.STATE_NEW, Task.STATE_RUNNING)) {
                                Executor executorForTask = task.executorOverride();
                                if (executorForTask == null) {
                                    executorForTask = defaultExecutor;
                                }

                                executorForTask.execute(() -> {
                                    LOG.runningTimerTask(task);
                                    try {
                                        task.runnable.run();
                                    } finally {
                                        STATE.setRelease(task, Task.STATE_FINISHED);
                                    }
                                });
                            }
                        } else {
                            // this is OK even if another timer is scheduled during the sleep (even if that timer should
                            // fire sooner than `taskStartTime`), because `schedule` always calls` LockSupport.unpark`
                            LockSupport.parkNanos(taskStartTime - currentTime);
                        }
                    }
                } catch (Exception e) {
                    // can happen e.g. when the executor is shut down sooner than the timer
                    LOG.unexpectedExceptionInTimerLoop(e);
                }
            }
        }, name);
        thread.start();
    }

    @Override
    public TimerTask schedule(long delayInMillis, Runnable task) {
        return schedule(delayInMillis, task, null);
    }

    @Override
    public TimerTask schedule(long delayInMillis, Runnable task, Executor executor) {
        long startTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(delayInMillis);
        Task timerTask = executor == null
            ? new Task(startTime, RunnableWrapper.INSTANCE.wrap(task), tasks)
            : new Task(startTime, RunnableWrapper.INSTANCE.wrap(task), tasks){
                @Override Executor executorOverride (){
                    return executor;
                }
            };
        tasks.add(timerTask);
        LockSupport.unpark(thread);
        LOG.scheduledTimerTask(timerTask, delayInMillis);
        return timerTask;
    }

    @Override
    public void shutdown() throws InterruptedException {
        if (running.compareAndSet(true, false)) {
            LOG.shutdownTimer(name);
            thread.interrupt();
            thread.join();
        }
    }

    private static class Task implements TimerTask {
        static final byte STATE_NEW = 0; // was scheduled, but isn't running yet
        static final byte STATE_RUNNING = 1; // running on the executor
        static final byte STATE_FINISHED = 2; // finished running
        static final byte STATE_CANCELLED = 3; // cancelled before it could be executed

        final long startTime; // in nanos, to be compared with System.nanoTime()
        final Runnable runnable;
        volatile byte state = STATE_NEW;

        private final SortedSet<Task> tasks;

        Task(long startTime, Runnable runnable, SortedSet<Task> tasks) {
            this.startTime = startTime;
            this.runnable = checkNotNull(runnable, "Runnable task must be set");
            this.tasks = checkNotNull(tasks, "Tasks-set must be set");
        }

        Executor executorOverride() {
            return null; // may be null, which means that the timer's executor shall be used
        }

        @Override
        public boolean isDone() {
            byte s = this.state;
            return s == STATE_FINISHED || s == STATE_CANCELLED;
        }

        @Override
        public boolean cancel() {
            // can't cancel if it's already running
            if (STATE.compareAndSet(this, STATE_NEW, STATE_CANCELLED)) {
                LOG.cancelledTimerTask(this);
                tasks.remove(this);
                return true;
            }
            return false;
        }
    }

    // VarHandle mechanics
    private static final VarHandle STATE;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            STATE = l.findVarHandle(Task.class, "state", byte.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
