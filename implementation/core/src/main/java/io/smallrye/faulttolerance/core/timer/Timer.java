package io.smallrye.faulttolerance.core.timer;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import org.jboss.logging.Logger;

/**
 * Allows scheduling tasks ({@code Runnable}s) to be executed on an {@code Executor} after some delay.
 * <p>
 * Starts one thread that processes submitted tasks in a loop and when it's time for a task to run,
 * it gets submitted to the executor.
 */
// TODO implement a hashed wheel?
public final class Timer {
    private static final Logger LOG = Logger.getLogger(Timer.class);

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private static final TimerRunnableWrapper RUNNABLE_WRAPPER = TimerRunnableWrapper.load();

    private static final Comparator<TimerTask> TIMER_TASK_COMPARATOR = (o1, o2) -> {
        if (o1 == o2) {
            // two different instances are never equal
            return 0;
        }

        // must _not_ return 0 if start times are equal, because that isn't consistent
        // with `equals` (see also above)
        return o1.startTime <= o2.startTime ? -1 : 1;
    };

    private final SortedSet<TimerTask> tasks;

    private final Thread thread;

    private final AtomicBoolean running = new AtomicBoolean(true);

    public Timer(Executor executor) {
        this.tasks = new ConcurrentSkipListSet<>(TIMER_TASK_COMPARATOR);
        this.thread = new Thread(() -> {
            while (running.get()) {
                try {
                    if (tasks.isEmpty()) {
                        LockSupport.park();
                    } else {
                        TimerTask task;
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
                            if (task.state.compareAndSet(TimerTask.STATE_NEW, TimerTask.STATE_RUNNING)) {
                                executor.execute(() -> {
                                    try {
                                        task.runnable.run();
                                    } finally {
                                        task.state.set(TimerTask.STATE_FINISHED);
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
                    LOG.warn("Unexpected exception in timer loop, ignoring", e);
                }
            }
        }, "SmallRye Fault Tolerance Timer " + COUNTER.incrementAndGet());
        thread.start();
    }

    public TimerTask schedule(long delayInMillis, Runnable runnable) {
        long startTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(delayInMillis);
        TimerTask task = new TimerTask(startTime, RUNNABLE_WRAPPER.wrap(runnable), tasks::remove);
        tasks.add(task);
        LockSupport.unpark(thread);
        return task;
    }

    /**
     * Should be called <i>before</i> the underlying {@code executor} is shut down.
     * Returns only after the timer thread finishes.
     */
    public void shutdown() throws InterruptedException {
        if (running.compareAndSet(true, false)) {
            thread.interrupt();
            thread.join();
        }
    }
}
