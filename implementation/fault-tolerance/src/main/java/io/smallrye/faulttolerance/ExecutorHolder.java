package io.smallrye.faulttolerance;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.smallrye.faulttolerance.core.timer.Timer;

@Singleton
public class ExecutorHolder {
    private final ExecutorService asyncExecutor;

    private final Timer timer;

    private final boolean shouldShutdownAsyncExecutor;

    @Inject
    public ExecutorHolder(AsyncExecutorProvider asyncExecutorProvider) {
        this.asyncExecutor = asyncExecutorProvider.get();
        this.timer = new Timer(asyncExecutor);
        this.shouldShutdownAsyncExecutor = asyncExecutorProvider instanceof DefaultAsyncExecutorProvider;
    }

    @PreDestroy
    public void tearDown() {
        try {
            timer.shutdown();
        } catch (InterruptedException e) {
            // no need to do anything, we're shutting down anyway
            // just set the interruption flag to be a good citizen
            Thread.currentThread().interrupt();
        }

        if (shouldShutdownAsyncExecutor) {
            asyncExecutor.shutdownNow();
            try {
                asyncExecutor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // no need to do anything, we're shutting down anyway
                // just set the interruption flag to be a good citizen
                Thread.currentThread().interrupt();
            }
        }
    }

    public ExecutorService getAsyncExecutor() {
        return asyncExecutor;
    }

    public Timer getTimer() {
        return timer;
    }
}
