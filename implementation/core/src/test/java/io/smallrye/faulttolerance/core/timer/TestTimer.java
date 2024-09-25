package io.smallrye.faulttolerance.core.timer;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

public class TestTimer implements Timer {
    private final Queue<Task> tasks = new ConcurrentLinkedQueue<>();

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public TimerTask schedule(long delayInMillis, Runnable runnable) {
        Task task = new Task(runnable);
        tasks.add(task);
        return task;
    }

    @Override
    public TimerTask schedule(long delayInMillis, Runnable runnable, Executor executor) {
        Task task = new Task(runnable);
        tasks.add(task);
        return task;
    }

    public boolean hasScheduledTasks() {
        return !tasks.isEmpty();
    }

    public TimerTask nextScheduledTask() {
        return tasks.peek();
    }

    public void executeSynchronously(TimerTask task) {
        if (tasks.remove(task)) {
            ((Task) task).runnable.run();
        }
    }

    @Override
    public int countScheduledTasks() {
        return tasks.size();
    }

    @Override
    public void shutdown() throws InterruptedException {
    }

    private class Task implements TimerTask {
        private final Runnable runnable;

        private Task(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public boolean isDone() {
            return tasks.contains(this);
        }

        @Override
        public boolean cancel() {
            return tasks.remove(this);
        }
    }
}
