package io.smallrye.faulttolerance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Provider of thread pools for timeouts and asynchronous invocations
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@Singleton
public class ExecutorProvider {

    @Inject
    @ConfigProperty(name = "io.smallrye.faulttolerance.globalThreadPoolSize", defaultValue = "100")
    private Integer size;

    @Inject
    @ConfigProperty(name = "io.smallrye.faulttolerance.timeoutExecutorThreads", defaultValue = "5")
    private Integer timeoutExecutorSize;

    private ExecutorService globalExecutor;

    private ScheduledExecutorService timeoutExecutor;

    private ExecutorFactory executorFactory;

    private final List<ExecutorService> allExecutors = new ArrayList<>();

    @PostConstruct
    public void setUp() {
        if (size < 5) {
            throw new IllegalArgumentException("Please set the global thread pool size for a value larger than 5. ");
        }
        executorFactory = executorProvider();
        // global executor cannot use queue, it could lead to e.g. thread that runs the future
        // waiting for the thread that waits for the future to be finished
        globalExecutor = executorFactory.createCoreExecutor(size);

        timeoutExecutor = executorFactory.createTimeoutExecutor(timeoutExecutorSize);
        allExecutors.add(globalExecutor);
        allExecutors.add(timeoutExecutor);
    }

    @PreDestroy
    public void tearDown() {
        allExecutors.forEach(ExecutorService::shutdownNow);

        for (ExecutorService executor : allExecutors) {
            try {
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                // no need to do anything, we're shutting down anyway
                // just set the interruption flag to be a good citizen
                Thread.currentThread().interrupt();
            }
        }
    }

    public ExecutorService createAdHocExecutor(int size) {
        ExecutorService executor = executorFactory.createExecutor(size, size);
        allExecutors.add(executor);
        return executor;
    }

    public ExecutorService getGlobalExecutor() {
        return globalExecutor;
    }

    public ScheduledExecutorService getTimeoutExecutor() {
        return timeoutExecutor;
    }

    private static ExecutorFactory executorProvider() {
        ServiceLoader<ExecutorFactory> loader = ServiceLoader.load(ExecutorFactory.class);

        Iterator<ExecutorFactory> iterator = loader.iterator();

        ExecutorFactory maxPriorityProvider = new DefaultExecutorFactory();
        while (iterator.hasNext()) {
            ExecutorFactory next = iterator.next();
            if (next.priority() > maxPriorityProvider.priority()) {
                maxPriorityProvider = next;
            }
        }
        return maxPriorityProvider;
    }
}
