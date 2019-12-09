package io.smallrye.faulttolerance;

import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@Singleton
public class ExecutorContainer { // mstodo name; it's more than a container
    // mstodo documment
    @Inject
    @ConfigProperty(name = "io.smallrye.faulttolerance.maxAsyncThreads", defaultValue = "100")
    private Integer size;
    @Inject
    @ConfigProperty(name = "io.smallrye.faulttolerance.maxAsyncThreadsWorkQueue")
    private Optional<Integer> queueSize;

    private ExecutorService executorService;
    private ExecutorFactory executorFactory;

    @PostConstruct
    public void setUp() {
        executorFactory = executorProvider();
        executorService = executorFactory.getGlobalExecutorService(size, queueSize.orElse(size));
    }

    public ExecutorService getAdHocExecutor(int size, int queueSize, BlockingQueue<Runnable> queue) {
        return executorFactory.createExecutorService(size, queueSize, queue);
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

    public ExecutorService getGlobalExecutor() {
        return executorService;
    }
}
