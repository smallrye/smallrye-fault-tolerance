package io.smallrye.faulttolerance.core.util;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class NamedFutureTask<T> extends FutureTask<T> {
    private final String name;

    public NamedFutureTask(String name, Callable<T> callable) {
        super(callable);
        this.name = name;
    }

    @Override
    public void run() {
        String oldName = Thread.currentThread().getName();
        String newName = name + "{" + oldName + "}"; // expects threads to come from thread pools
        Thread.currentThread().setName(newName);
        try {
            super.run();
        } finally {
            Thread.currentThread().setName(oldName);
        }
    }
}
